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

package org.springframework.cloud.context.properties;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.cloud.util.ProxyUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Listens for {@link EnvironmentChangeEvent} and rebinds beans that were bound to the
 * {@link Environment} using {@link ConfigurationProperties
 * <code>@ConfigurationProperties</code>}. When these beans are re-bound and
 * re-initialized, the changes are available immediately to any component that is using
 * the <code>@ConfigurationProperties</code> bean.
 *
 * @see RefreshScope for a deeper and optionally more focused refresh of bean components.
 * @author Dave Syer
 *
 */
@Component
@ManagedResource
public class ConfigurationPropertiesRebinder
		implements ApplicationContextAware, ApplicationListener<EnvironmentChangeEvent> {

	private ConfigurationPropertiesBeans beans;

	private ApplicationContext applicationContext;

	private Map<String, Exception> errors = new ConcurrentHashMap<>();

	public ConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
		this.beans = beans;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * A map of bean name to errors when instantiating the bean.
	 * @return The errors accumulated since the latest destroy.
	 */
	public Map<String, Exception> getErrors() {
		return this.errors;
	}

	@ManagedOperation
	public void rebind() {
		this.errors.clear();
		for (String name : this.beans.getBeanNames()) {
			rebind(name);
		}
	}

	@ManagedOperation
	public boolean rebind(String name) {
		if (!this.beans.getBeanNames().contains(name)) {
			return false;
		}
		ApplicationContext appContext = this.applicationContext;
		while (appContext != null) {
			if (appContext.containsLocalBean(name)) {
				return rebind(name, appContext);
			}
			else {
				appContext = appContext.getParent();
			}
		}
		return false;
	}

	/**
	 * WARNING: This method rebinds beans from any context in the hierarchy using the main
	 * application context.
	 * @param type bean type to rebind.
	 * @return true, if successful.
	 */
	public boolean rebind(Class type) {
		String[] beanNamesForType = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this.applicationContext, type);
		if (beanNamesForType.length > 0) {
			String name = beanNamesForType[0];
			if (ScopedProxyUtils.isScopedTarget(name)) {
				name = ScopedProxyUtils.getOriginalBeanName(name);
			}
			return rebind(name, this.applicationContext);
		}
		return false;
	}

	private boolean rebind(String name, ApplicationContext appContext) {
		try {
			Object bean = appContext.getBean(name);
			if (AopUtils.isAopProxy(bean)) {
				bean = ProxyUtils.getTargetObject(bean);
			}
			if (bean != null) {
				// TODO: determine a more general approach to fix this.
				// see
				// https://github.com/spring-cloud/spring-cloud-commons/issues/571
				if (getNeverRefreshable().contains(bean.getClass().getName())) {
					return false; // ignore
				}
				appContext.getAutowireCapableBeanFactory().destroyBean(bean);
				appContext.getAutowireCapableBeanFactory().initializeBean(bean, name);
				return true;
			}
		}
		catch (RuntimeException e) {
			this.errors.put(name, e);
			throw e;
		}
		catch (Exception e) {
			this.errors.put(name, e);
			throw new IllegalStateException("Cannot rebind to " + name, e);
		}
		return false;
	}

	@ManagedAttribute
	public Set<String> getNeverRefreshable() {
		String neverRefresh = this.applicationContext.getEnvironment()
				.getProperty("spring.cloud.refresh.never-refreshable", "com.zaxxer.hikari.HikariDataSource");
		return StringUtils.commaDelimitedListToSet(neverRefresh);
	}

	@ManagedAttribute
	public Set<String> getBeanNames() {
		return new HashSet<>(this.beans.getBeanNames());
	}

	@Override
	public void onApplicationEvent(EnvironmentChangeEvent event) {
		if (this.applicationContext.equals(event.getSource())
				// Backwards compatible
				|| event.getKeys().equals(event.getSource())) {
			rebind();
		}
	}

}
