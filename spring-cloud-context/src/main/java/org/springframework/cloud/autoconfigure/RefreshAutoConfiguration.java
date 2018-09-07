/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.autoconfigure;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.endpoint.event.RefreshEventListener;
import org.springframework.cloud.logging.LoggingRebinder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.stereotype.Component;

/**
 * Autoconfiguration for the refresh scope and associated features to do with changes in
 * the Environment (e.g. rebinding logger levels).
 *
 * @author Dave Syer
 * @author Venil Noronha
 */
@Configuration
@ConditionalOnClass(RefreshScope.class)
@ConditionalOnProperty(name = RefreshAutoConfiguration.REFRESH_SCOPE_ENABLED, matchIfMissing = true)
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
public class RefreshAutoConfiguration {

	public static final String REFRESH_SCOPE_NAME = "refresh";
	public static final String REFRESH_SCOPE_PREFIX = "spring.cloud.refresh";
	public static final String REFRESH_SCOPE_ENABLED = REFRESH_SCOPE_PREFIX + ".enabled";

	@Bean
	@ConditionalOnMissingBean(RefreshScope.class)
	public static RefreshScope refreshScope() {
		return new RefreshScope();
	}

	@Configuration
	@ConditionalOnClass(name = "javax.persistence.EntityManagerFactory")
	protected static class JpaInvokerConfiguration implements LoadTimeWeaverAware {

		@Autowired
		private ListableBeanFactory beanFactory;

		@PostConstruct
		public void init() {
			String cls = "org.springframework.boot.autoconfigure.jdbc.DataSourceInitializerInvoker";
			if (beanFactory.containsBean(cls)) {
				beanFactory.getBean(cls);
			}
		}

		@Override
		public void setLoadTimeWeaver(LoadTimeWeaver ltw) {
		}

	}

	@Component
	protected static class RefreshScopeBeanDefinitionEnhancer
			implements BeanPostProcessor, BeanDefinitionRegistryPostProcessor {

		private BeanDefinitionRegistry registry;

		/**
		 * Class names for beans to post process into refresh scope. Useful when you don't
		 * control the bean definition (e.g. it came from auto-configuration).
		 */
		private Set<String> refreshables = new HashSet<>(
				Arrays.asList("com.zaxxer.hikari.HikariDataSource"));

		public Set<String> getRefreshable() {
			return this.refreshables;
		}

		public void setRefreshable(Set<String> refreshables) {
			if (this.refreshables != refreshables) {
				this.refreshables.clear();
				this.refreshables.addAll(refreshables);
			}
		}

		public void setExtraRefreshable(Set<String> refreshables) {
			this.refreshables.addAll(refreshables);
		}

		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
			Environment environment = beanFactory.getBean(Environment.class);
			if (environment == null) {
				environment = new StandardEnvironment();
			}
			Binder.get(environment).bind(REFRESH_SCOPE_PREFIX, Bindable.ofInstance(this));
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
				throws BeansException {
			this.registry = registry;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			BeanDefinition definition = null;
			try {
				definition = registry.getBeanDefinition(beanName);
			}
			catch (NoSuchBeanDefinitionException e) {
				// just ignore and move on
				return bean;
			}
			if (isApplicable(registry, beanName, definition)) {
				definition.setScope(REFRESH_SCOPE_NAME);
				ProxyFactory proxyFactory = new ProxyFactory(bean);
				return proxyFactory.getProxy();
			}
			return bean;
		}

		private boolean isApplicable(BeanDefinitionRegistry registry, String name,
				BeanDefinition definition) {
			String scope = definition.getScope();
			if (REFRESH_SCOPE_NAME.equals(scope)) {
				// Already refresh scoped
				return false;
			}
			String type = definition.getBeanClassName();
			if (registry instanceof BeanFactory) {
				Class<?> cls = ((BeanFactory) registry).getType(name);
				if (cls != null) {
					type = cls.getName();
				}
			}
			if (type != null) {
				return this.refreshables.contains(type);
			}
			return false;
		}

	}

	@Bean
	@ConditionalOnMissingBean
	public static LoggingRebinder loggingRebinder() {
		return new LoggingRebinder();
	}

	@Bean
	@ConditionalOnMissingBean
	public ContextRefresher contextRefresher(ConfigurableApplicationContext context,
			RefreshScope scope) {
		return new ContextRefresher(context, scope);
	}

	@Bean
	public RefreshEventListener refreshEventListener(ContextRefresher contextRefresher) {
		return new RefreshEventListener(contextRefresher);
	}

}
