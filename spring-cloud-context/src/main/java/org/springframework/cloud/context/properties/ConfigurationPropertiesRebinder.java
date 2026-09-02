/*
 * Copyright 2012-present the original author or authors.
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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration.RefreshProperties;
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
 * <p>
 * Rebinding a given bean is serialized against other concurrent rebinds of that same
 * bean, so two overlapping refreshes (for example a manual {@link #rebind(String)} call
 * racing with an {@link EnvironmentChangeEvent}-triggered {@link #rebind()}) cannot
 * interleave their destroy/reset/re-initialize steps. That does not, however, make the
 * bean safe to read concurrently from other threads while a rebind is in progress: the
 * bean is mutated in place, field by field, so a concurrent reader can observe transient
 * intermediate state. Beans that need a consistent view across a refresh should use
 * {@link RefreshScope} instead, which serializes reads against refreshes via a per-bean
 * {@link java.util.concurrent.locks.ReadWriteLock}.
 *
 * @author Dave Syer
 * @author Yanming Zhou
 * @see RefreshScope for a deeper and optionally more focused refresh of bean components.
 *
 */
@Component
@ManagedResource
public class ConfigurationPropertiesRebinder
		implements ApplicationContextAware, ApplicationListener<EnvironmentChangeEvent> {

	private static final Log logger = LogFactory.getLog(ConfigurationPropertiesRebinder.class);

	private ConfigurationPropertiesBeans beans;

	private ApplicationContext applicationContext;

	private Map<String, Exception> errors = new ConcurrentHashMap<>();

	private final ConcurrentMap<String, Lock> rebindLocks = new ConcurrentHashMap<>();

	private final Set<String> neverResetNestedTypes;

	public ConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans) {
		this(beans, Collections.emptySet());
	}

	public ConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans, RefreshProperties refreshProperties) {
		this(beans, refreshProperties.getNeverResetNestedTypes());
	}

	private ConfigurationPropertiesRebinder(ConfigurationPropertiesBeans beans, Set<String> neverResetNestedTypes) {
		this.beans = beans;
		this.neverResetNestedTypes = neverResetNestedTypes;
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
		// Serialize concurrent rebinds of the *same* bean (for example a manual
		// rebind(name) racing with an EnvironmentChangeEvent-triggered rebind()) so
		// their destroy/reset/re-initialize steps cannot interleave on the live bean.
		// This does not protect concurrent readers of the bean; see the class Javadoc.
		Lock lock = this.rebindLocks.computeIfAbsent(name, key -> new ReentrantLock());
		lock.lock();
		try {
			try {
				Object bean = appContext.getBean(name);
				if (bean != null) {
					Class<?> targetClass = AopUtils.getTargetClass(bean);
					// TODO: determine a more general approach to fix this.
					// see
					// https://github.com/spring-cloud/spring-cloud-commons/issues/571
					if (getNeverRefreshable().contains(targetClass.getName()) || getNeverRefreshable().contains(name)) {
						return false; // ignore
					}
					if (AopUtils.isAopProxy(bean) && bean instanceof Advised advised) {
						Object target = ProxyUtils.getTargetObject(bean);
						if (target != bean && !targetClass.isInterface()
								&& !Modifier.isAbstract(targetClass.getModifiers())) {
							Object freshBean = appContext.getAutowireCapableBeanFactory().createBean(targetClass);
							Object freshTarget = AopUtils.isAopProxy(freshBean) ? ProxyUtils.getTargetObject(freshBean)
									: freshBean;
							advised.setTargetSource(new SingletonTargetSource(freshTarget));
							appContext.getAutowireCapableBeanFactory().destroyBean(target);
						}
						else {
							appContext.getAutowireCapableBeanFactory().destroyBean(target);
							resetBeanToDefaults(target);
							appContext.getAutowireCapableBeanFactory().autowireBean(target);
							appContext.getAutowireCapableBeanFactory().initializeBean(target, name);
						}
					}
					else {
						appContext.getAutowireCapableBeanFactory().destroyBean(bean);
						resetBeanToDefaults(bean);
						appContext.getAutowireCapableBeanFactory().autowireBean(bean);
						appContext.getAutowireCapableBeanFactory().initializeBean(bean, name);
					}
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
		finally {
			lock.unlock();
		}
	}

	/**
	 * Reset bean properties to their class-level defaults so that removed properties do
	 * not retain stale values after rebinding.
	 */
	private void resetBeanToDefaults(Object bean) {
		Class<?> targetClass = AopUtils.getTargetClass(bean);
		if (!hasDefaultConstructor(targetClass)) {
			// Beans whose no-arg constructor (if any) is not their only constructor
			// (for example constructor-bound beans, beans with required dependencies, or
			// beans with an extra no-arg constructor kept only for a framework's benefit)
			// cannot be instantiated to obtain trustworthy defaults, so the reset is
			// skipped. The bean is still re-bound from the Environment afterwards; only
			// reverting removed properties to their defaults is skipped.
			if (logger.isDebugEnabled()) {
				logger.debug("No default constructor for " + targetClass.getName()
						+ "; skipping property reset before rebinding");
			}
			return;
		}
		Object freshInstance;
		try {
			freshInstance = BeanUtils.instantiateClass(targetClass);
		}
		catch (Exception ex) {
			logger.warn("Cannot create default instance of " + targetClass.getName()
					+ " for reset; skipping property reset", ex);
			return;
		}
		resetProperties(bean, freshInstance, Collections.newSetFromMap(new IdentityHashMap<>()));
	}

	/**
	 * Whether the given type's <em>only</em> declared constructor is a no-argument one,
	 * which is what {@link BeanUtils#instantiateClass(Class)} needs to build a defaults
	 * template that is actually representative of the bean's defaults.
	 * <p>
	 * A no-arg constructor that sits alongside another, parameterized constructor is
	 * deliberately not enough here, regardless of its visibility: some beans declare one
	 * purely for a framework's benefit (for example Jackson deserialization) next to
	 * another constructor that takes collaborators and computes the bean's real defaults
	 * (for example from the current host). Instantiating the type through the no-arg
	 * constructor then produces a template with the wrong defaults - typically
	 * {@code null} or zero-valued fields - for anything the other constructor would have
	 * computed, and resetting the bean to that template before rebinding wipes out values
	 * no property source ever carried (see gh-1733). Only a type whose sole constructor
	 * takes no arguments can be assumed to have one intended, complete way of producing a
	 * default instance.
	 */
	private boolean hasDefaultConstructor(Class<?> type) {
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		return constructors.length == 1 && constructors[0].getParameterCount() == 0;
	}

	private void resetProperties(Object bean, Object defaults, Set<Object> visited) {
		// Guard against cyclic object graphs so that recursion always terminates.
		if (bean == null || !visited.add(bean)) {
			return;
		}
		BeanWrapper target = new BeanWrapperImpl(bean);
		BeanWrapper defaultsWrapper = new BeanWrapperImpl(defaults);
		for (PropertyDescriptor pd : target.getPropertyDescriptors()) {
			String propertyName = pd.getName();
			if ("class".equals(propertyName)) {
				continue;
			}
			try {
				if (target.isWritableProperty(propertyName) && defaultsWrapper.isReadableProperty(propertyName)) {
					Object defaultValue = defaultsWrapper.getPropertyValue(propertyName);
					target.setPropertyValue(propertyName, defaultValue);
				}
				else if (target.isReadableProperty(propertyName) && defaultsWrapper.isReadableProperty(propertyName)) {
					Object value = target.getPropertyValue(propertyName);
					Object defaultValue = defaultsWrapper.getPropertyValue(propertyName);
					if (value instanceof Collection collection) {
						collection.clear();
						if (defaultValue instanceof Collection defaultCollection) {
							collection.addAll(defaultCollection);
						}
					}
					else if (value instanceof Map map) {
						map.clear();
						if (defaultValue instanceof Map defaultMap) {
							map.putAll(defaultMap);
						}
					}
					else if (value != null && defaultValue != null && isResettableNestedType(value.getClass())) {
						resetProperties(value, defaultValue, visited);
					}
				}
			}
			catch (Exception ex) {
				if (logger.isDebugEnabled()) {
					logger.debug("Failed to reset property '" + propertyName + "' on "
							+ AopUtils.getTargetClass(bean).getName(), ex);
				}
			}
		}
	}

	/**
	 * Determine whether a nested property value should be recursively reset. Only
	 * user-defined types are descended into. Recursing into JDK or standard API types
	 * (for example {@link javax.net.ssl.SSLContext}) is both unnecessary and unsafe:
	 * their object graphs may be cyclic, which previously led to a
	 * {@link StackOverflowError} (see gh-1698), and they may expose internal collections
	 * or maps that must not be cleared. Additional types can be excluded via the
	 * {@code spring.cloud.refresh.never-reset-nested-types} property.
	 */
	boolean isResettableNestedType(Class<?> type) {
		if (type.isArray() || BeanUtils.isSimpleValueType(type) || isJdkClass(type) || isStandardApiClass(type)) {
			return false;
		}
		String typeName = type.getName();
		for (String excluded : this.neverResetNestedTypes) {
			if (typeName.startsWith(excluded)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether the given type is provided by the JDK itself. JDK types are loaded either
	 * by the bootstrap class loader (for example everything in {@code java.base}, which
	 * reports a {@code null} class loader) or by the platform class loader (the other
	 * platform modules); application and library types are loaded by the application
	 * class loader.
	 */
	private boolean isJdkClass(Class<?> type) {
		ClassLoader classLoader = type.getClassLoader();
		return classLoader == null || classLoader == ClassLoader.getPlatformClassLoader();
	}

	/**
	 * Whether the given type belongs to a standard API namespace (Jakarta EE or the
	 * legacy {@code javax} packages). These namespaces are reserved for specifications
	 * and, unlike the JDK modules, are loaded by the application class loader, so they
	 * are not detected by {@link #isJdkClass}.
	 */
	private boolean isStandardApiClass(Class<?> type) {
		String packageName = type.getPackageName();
		return packageName.startsWith("jakarta.") || packageName.startsWith("javax.");
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
