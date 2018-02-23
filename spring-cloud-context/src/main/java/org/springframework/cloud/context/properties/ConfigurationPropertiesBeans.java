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
 */
package org.springframework.cloud.context.properties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationBeanFactoryMetadata;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

/**
 * Collects references to <code>@ConfigurationProperties</code> beans in the context and
 * its parent.
 *
 * @author Dave Syer
 *
 */
@Component
public class ConfigurationPropertiesBeans implements BeanPostProcessor,
ApplicationContextAware {

	private ConfigurationBeanFactoryMetadata metaData;

	private Map<String, Object> beans = new HashMap<String, Object>();

	private ConfigurableListableBeanFactory beanFactory;

	private String refreshScope;

	private boolean refreshScopeInitialized;

	private ConfigurationPropertiesBeans parent;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		if (applicationContext.getAutowireCapableBeanFactory() instanceof ConfigurableListableBeanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) applicationContext
					.getAutowireCapableBeanFactory();
		}
		if (applicationContext.getParent() != null
				&& applicationContext.getParent().getAutowireCapableBeanFactory() instanceof ConfigurableListableBeanFactory) {
			ConfigurableListableBeanFactory listable = (ConfigurableListableBeanFactory) applicationContext
					.getParent().getAutowireCapableBeanFactory();
			String[] names = listable
					.getBeanNamesForType(ConfigurationPropertiesBeans.class);
			if (names.length == 1) {
				this.parent = (ConfigurationPropertiesBeans) listable.getBean(names[0]);
				this.beans.putAll(this.parent.beans);
			}
		}
	}

	/**
	 * @param beans the bean meta data to set
	 */
	public void setBeanMetaDataStore(ConfigurationBeanFactoryMetadata beans) {
		this.metaData = beans;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		if (isRefreshScoped(beanName)) {
			return bean;
		}
		ConfigurationProperties annotation = AnnotationUtils.findAnnotation(
				bean.getClass(), ConfigurationProperties.class);
		if (annotation != null) {
			this.beans.put(beanName, bean);
		}
		else if (this.metaData != null) {
			annotation = this.metaData.findFactoryAnnotation(beanName,
					ConfigurationProperties.class);
			if (annotation != null) {
				this.beans.put(beanName, bean);
			}
		}
		return bean;
	}

	private boolean isRefreshScoped(String beanName) {
		if (this.refreshScope == null && !this.refreshScopeInitialized) {
			this.refreshScopeInitialized = true;
			for (String scope : this.beanFactory.getRegisteredScopeNames()) {
				if (this.beanFactory.getRegisteredScope(scope) instanceof org.springframework.cloud.context.scope.refresh.RefreshScope) {
					this.refreshScope = scope;
					break;
				}
			}
		}
		if (beanName == null || this.refreshScope == null) {
			return false;
		}
		return this.beanFactory.containsBeanDefinition(beanName)
				&& this.refreshScope.equals(this.beanFactory.getBeanDefinition(beanName)
						.getScope());
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	public Set<String> getBeanNames() {
		return new HashSet<String>(this.beans.keySet());
	}

}
