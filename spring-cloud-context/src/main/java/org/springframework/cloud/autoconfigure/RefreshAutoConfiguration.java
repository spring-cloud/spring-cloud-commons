/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.autoconfigure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.cloud.context.refresh.ConfigDataContextRefresher;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.cloud.context.refresh.LegacyContextRefresher;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.endpoint.event.RefreshEventListener;
import org.springframework.cloud.logging.LoggingRebinder;
import org.springframework.cloud.util.ConditionalOnBootstrapDisabled;
import org.springframework.cloud.util.ConditionalOnBootstrapEnabled;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.weaving.LoadTimeWeaverAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.style.ToStringCreator;
import org.springframework.instrument.classloading.LoadTimeWeaver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Autoconfiguration for the refresh scope and associated features to do with changes in
 * the Environment (e.g. rebinding logger levels).
 *
 * @author Dave Syer
 * @author Venil Noronha
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(RefreshScope.class)
@ConditionalOnProperty(name = RefreshAutoConfiguration.REFRESH_SCOPE_ENABLED, matchIfMissing = true)
@AutoConfigureBefore(HibernateJpaAutoConfiguration.class)
@EnableConfigurationProperties(RefreshAutoConfiguration.RefreshProperties.class)
public class RefreshAutoConfiguration {

	/**
	 * Name of the refresh scope name.
	 */
	public static final String REFRESH_SCOPE_NAME = "refresh";

	/**
	 * Name of the prefix for refresh scope.
	 */
	public static final String REFRESH_SCOPE_PREFIX = "spring.cloud.refresh";

	/**
	 * Name of the enabled prefix for refresh scope.
	 */
	public static final String REFRESH_SCOPE_ENABLED = REFRESH_SCOPE_PREFIX + ".enabled";

	@Bean
	@ConditionalOnMissingBean(RefreshScope.class)
	public static RefreshScope refreshScope() {
		return new RefreshScope();
	}

	@Bean
	@ConditionalOnMissingBean
	public static LoggingRebinder loggingRebinder() {
		return new LoggingRebinder();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBootstrapEnabled
	public LegacyContextRefresher legacyContextRefresher(ConfigurableApplicationContext context, RefreshScope scope,
			RefreshProperties properties) {
		return new LegacyContextRefresher(context, scope, properties);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBootstrapDisabled
	public ConfigDataContextRefresher configDataContextRefresher(ConfigurableApplicationContext context,
			RefreshScope scope, RefreshProperties properties) {
		return new ConfigDataContextRefresher(context, scope, properties);
	}

	@Bean
	public RefreshEventListener refreshEventListener(ContextRefresher contextRefresher) {
		return new RefreshEventListener(contextRefresher);
	}

	@ConfigurationProperties("spring.cloud.refresh")
	public static class RefreshProperties {

		/**
		 * Additional property sources to retain during a refresh. Typically only system
		 * property sources are retained. This property allows property sources, such as
		 * property sources created by EnvironmentPostProcessors to be retained as well.
		 */
		private List<String> additionalPropertySourcesToRetain;

		public List<String> getAdditionalPropertySourcesToRetain() {
			return this.additionalPropertySourcesToRetain;
		}

		public void setAdditionalPropertySourcesToRetain(List<String> additionalPropertySourcesToRetain) {
			this.additionalPropertySourcesToRetain = additionalPropertySourcesToRetain;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this)
					.append("additionalPropertySourcesToRetain", additionalPropertySourcesToRetain).toString();

		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "javax.persistence.EntityManagerFactory")
	protected static class JpaInvokerConfiguration implements LoadTimeWeaverAware {

		@Autowired
		private ListableBeanFactory beanFactory;

		@PostConstruct
		public void init() {
			String cls = "org.springframework.boot.autoconfigure.jdbc.DataSourceInitializerInvoker";
			if (this.beanFactory.containsBean(cls)) {
				this.beanFactory.getBean(cls);
			}
		}

		@Override
		public void setLoadTimeWeaver(LoadTimeWeaver ltw) {
		}

	}

	@Component
	protected static class RefreshScopeBeanDefinitionEnhancer
			implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

		private Environment environment;

		private boolean bound = false;

		/**
		 * Class names for beans to post process into refresh scope. Useful when you don't
		 * control the bean definition (e.g. it came from auto-configuration).
		 */
		private Set<String> refreshables = new HashSet<>();

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
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		}

		@Override
		public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
			bindEnvironmentIfNeeded(registry);
			for (String name : registry.getBeanDefinitionNames()) {
				BeanDefinition definition = registry.getBeanDefinition(name);
				if (isApplicable(registry, name, definition)) {
					BeanDefinitionHolder holder = new BeanDefinitionHolder(definition, name);
					BeanDefinitionHolder proxy = ScopedProxyUtils.createScopedProxy(holder, registry, true);
					definition.setScope("refresh");
					if (registry.containsBeanDefinition(proxy.getBeanName())) {
						registry.removeBeanDefinition(proxy.getBeanName());
					}
					registry.registerBeanDefinition(proxy.getBeanName(), proxy.getBeanDefinition());
				}
			}
		}

		private boolean isApplicable(BeanDefinitionRegistry registry, String name, BeanDefinition definition) {
			String scope = definition.getScope();
			if (REFRESH_SCOPE_NAME.equals(scope)) {
				// Already refresh scoped
				return false;
			}
			String type = definition.getBeanClassName();
			if (!StringUtils.hasText(type) && registry instanceof BeanFactory) {
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

		private void bindEnvironmentIfNeeded(BeanDefinitionRegistry registry) {
			if (!this.bound) { // only bind once
				if (this.environment == null) {
					this.environment = new StandardEnvironment();
				}
				Binder.get(this.environment).bind("spring.cloud.refresh", Bindable.ofInstance(this));
				this.bound = true;
			}
		}

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

	}

}
