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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.SingletonTargetSource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindingPostProcessor;
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
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
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
 * bean is mutated in place, field by field, so a concurrent reader can observe a mix of
 * previous and new values. What it does not observe is unbound state: a fresh instance is
 * bound first and its values are then copied into the live bean, so a scalar never passes
 * through its class-level default, a nested object never through {@code null}, and a
 * getter-only collection or map is brought to its new contents without being emptied
 * first (the bean is destroyed, re-autowired and re-initialized afterwards, but not bound
 * in place a second time). Beans that need a consistent view across a refresh should use
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

	private final Set<String> loggedResetFailures = ConcurrentHashMap.newKeySet();

	/**
	 * Safety net against unbounded recursion in {@link #resetProperties}. Cyclic graphs
	 * are already broken by the identity-based {@code visited} set, and graphs built from
	 * unstable (newly-allocated-per-call) instances are broken by
	 * {@link #isStableInstance(BeanWrapper, String, Object)}; this limit only guards
	 * against a gap in either of those checks, or a legitimately very deep object graph.
	 */
	private static final int MAX_RESET_DEPTH = 25;

	private static final Set<String> INJECTION_ANNOTATIONS = Set.of(
			"org.springframework.beans.factory.annotation.Autowired",
			"org.springframework.beans.factory.annotation.Value", "jakarta.inject.Inject", "javax.inject.Inject",
			"jakarta.annotation.Resource", "javax.annotation.Resource");

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
				if (bean != null && !AopUtils.isAopProxy(bean)) {
					// A BeanPostProcessor may have replaced the bean with a wrapper after
					// the properties were bound (for example Boot's lazy connection fetch
					// wraps the DataSource). Rebind what the properties were bound to,
					// not
					// the wrapper, which could not be bound to the bean's declared type.
					Object bound = this.beans.getInstance(name);
					if (bound != null) {
						bean = bound;
					}
				}
				if (bean != null) {
					// The class behind every proxy layer, not just the outermost one
					Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
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
							// Keep the advice of every proxy layer
							innermostAdvised(advised).setTargetSource(new SingletonTargetSource(freshTarget));
							appContext.getAutowireCapableBeanFactory().destroyBean(target);
						}
						else {
							rebindInPlace(target, name, appContext);
						}
					}
					else {
						rebindInPlace(bean, name, appContext);
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
	 * Find the innermost proxy of a, possibly nested, chain of AOP proxies: the one whose
	 * target is not itself a proxy.
	 */
	private Advised innermostAdvised(Advised advised) throws Exception {
		Advised current = advised;
		Object next = current.getTargetSource().getTarget();
		while (AopUtils.isAopProxy(next) && next instanceof Advised nested) {
			current = nested;
			next = current.getTargetSource().getTarget();
		}
		return current;
	}

	/**
	 * Rebind the live bean in place. Its new state is bound on a fresh instance first and
	 * then copied over, so that readers never observe unbound state; the bean is then
	 * destroyed, re-autowired and re-initialized, without being bound in place a second
	 * time. A bean that cannot be reset from a fresh instance is destroyed and
	 * re-initialized as before, which binds it in place.
	 */
	private void rebindInPlace(Object bean, String name, ApplicationContext appContext) throws Exception {
		Object freshInstance = bindFreshInstance(bean, name, appContext);
		AutowireCapableBeanFactory beanFactory = appContext.getAutowireCapableBeanFactory();
		beanFactory.destroyBean(bean);
		if (freshInstance == null) {
			beanFactory.autowireBean(bean);
			beanFactory.initializeBean(bean, name);
			return;
		}
		copyFreshInstance(bean, freshInstance);
		beanFactory.autowireBean(bean);
		initializeWithoutBinding(bean, name, beanFactory);
	}

	/**
	 * Run the initialization callbacks of the live bean again, as
	 * {@link AutowireCapableBeanFactory#initializeBean} would, but without
	 * {@link ConfigurationPropertiesBindingPostProcessor}: the bean already holds the
	 * state bound on the fresh instance, and binding it in place would clear and refill
	 * its getter-only collections (Spring Boot's {@code CollectionBinder} merges into an
	 * existing collection that way), which is exactly the kind of transient state this
	 * class avoids.
	 */
	private void initializeWithoutBinding(Object bean, String name, AutowireCapableBeanFactory beanFactory)
			throws Exception {
		if (!(beanFactory instanceof AbstractBeanFactory abstractBeanFactory)) {
			beanFactory.initializeBean(bean, name);
			return;
		}
		for (BeanPostProcessor processor : abstractBeanFactory.getBeanPostProcessors()) {
			if (!(processor instanceof ConfigurationPropertiesBindingPostProcessor)
					&& processor.postProcessBeforeInitialization(bean, name) == null) {
				return;
			}
		}
		if (bean instanceof InitializingBean initializingBean) {
			initializingBean.afterPropertiesSet();
		}
		invokeInitMethods(bean, name, abstractBeanFactory);
		for (BeanPostProcessor processor : abstractBeanFactory.getBeanPostProcessors()) {
			if (processor.postProcessAfterInitialization(bean, name) == null) {
				return;
			}
		}
	}

	private void invokeInitMethods(Object bean, String name, AbstractBeanFactory beanFactory) {
		String[] initMethodNames;
		try {
			initMethodNames = (beanFactory.getMergedBeanDefinition(name) instanceof AbstractBeanDefinition definition)
					? definition.getInitMethodNames() : null;
		}
		catch (NoSuchBeanDefinitionException ex) {
			return;
		}
		if (initMethodNames == null) {
			return;
		}
		for (String initMethodName : initMethodNames) {
			Method initMethod = ReflectionUtils.findMethod(AopUtils.getTargetClass(bean), initMethodName);
			if (initMethod != null) {
				ReflectionUtils.makeAccessible(initMethod);
				ReflectionUtils.invokeMethod(initMethod, bean);
			}
		}
	}

	/**
	 * Create a fresh instance of the bean's class and bind it from the
	 * {@link Environment}, so that its state can replace the live bean's state once the
	 * live bean has been destroyed. This happens before the live bean is touched: a
	 * binding failure surfaces as the rebind error and leaves the live bean as it was.
	 * The instance is not autowired; injected properties are left alone by the copy.
	 * Returns {@code null} when no trustworthy fresh instance can be created; the live
	 * bean is then re-bound in place and removed properties keep their stale values.
	 */
	private @Nullable Object bindFreshInstance(Object bean, String name, ApplicationContext appContext) {
		ConfigurationPropertiesBindingPostProcessor binder = appContext
			.getBeanProvider(ConfigurationPropertiesBindingPostProcessor.class)
			.getIfAvailable();
		if (binder == null) {
			// Without the binder the fresh instance would only carry its class-level
			// defaults, and copying those onto the live bean would discard its values.
			if (logger.isDebugEnabled()) {
				logger.debug("No ConfigurationPropertiesBindingPostProcessor available; skipping property reset "
						+ "before rebinding");
			}
			return null;
		}
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
			return null;
		}
		Object freshInstance;
		try {
			freshInstance = BeanUtils.instantiateClass(targetClass);
		}
		catch (Exception ex) {
			logger.warn("Cannot create default instance of " + targetClass.getName()
					+ " for reset; skipping property reset", ex);
			return null;
		}
		binder.postProcessBeforeInitialization(freshInstance, name);
		return freshInstance;
	}

	/**
	 * Replace the properties of the live bean with those of the freshly bound instance,
	 * so that removed properties revert to their defaults while concurrent readers never
	 * observe the bean's unbound class-level defaults (see gh-1727).
	 */
	private void copyFreshInstance(Object bean, @Nullable Object freshInstance) {
		if (freshInstance != null) {
			resetProperties(bean, freshInstance, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
		}
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

	private void resetProperties(Object bean, Object freshInstance, Set<Object> visited, int depth) {
		// Guard against cyclic object graphs and pathologically deep ones so that
		// recursion
		// always terminates; see MAX_RESET_DEPTH.
		if (bean == null || depth > MAX_RESET_DEPTH || !visited.add(bean)) {
			return;
		}
		BeanWrapper target = new BeanWrapperImpl(bean);
		BeanWrapper freshWrapper = new BeanWrapperImpl(freshInstance);
		for (PropertyDescriptor pd : target.getPropertyDescriptors()) {
			String propertyName = pd.getName();
			if ("class".equals(propertyName)) {
				continue;
			}
			try {
				// A property whose declared type the user excluded is left alone whatever
				// its accessors look like: a read-only property is not descended into and
				// a writable one is not overwritten with the fresh instance's value.
				if (isNeverReset(pd.getPropertyType())) {
					continue;
				}
				if (target.isWritableProperty(propertyName)
						&& (freshWrapper.isReadableProperty(propertyName) || hasField(bean, propertyName))) {
					// A setter that is injected (for example @Autowired or @Value) is not
					// a
					// bound property: the fresh instance is not autowired, so copying it
					// would null the dependency before it is re-injected.
					if (isInjected(bean, pd) || (target.isReadableProperty(propertyName)
							&& isNeverReset(target.getPropertyValue(propertyName)))) {
						continue;
					}
					// A write-only property (a setter without a getter, which the binder
					// still binds) is read from its backing field on both instances.
					Object freshValue = readProperty(freshWrapper, freshInstance, propertyName);
					Object currentValue = readProperty(target, bean, propertyName);
					// Skip the setter call entirely when there's nothing to change: this
					// avoids needlessly invoking setters that don't tolerate being called
					// again with the same value, and setters that reject null are only
					// ever
					// invoked when the property actually needs to become null.
					if (!ObjectUtils.nullSafeEquals(currentValue, freshValue)) {
						try {
							target.setPropertyValue(propertyName, freshValue);
						}
						catch (Exception ex) {
							warnCannotReset(bean, propertyName, ex);
						}
					}
				}
				else if (target.isReadableProperty(propertyName) && freshWrapper.isReadableProperty(propertyName)) {
					Object value = target.getPropertyValue(propertyName);
					Object freshValue = freshWrapper.getPropertyValue(propertyName);
					if (value instanceof Collection collection) {
						replaceContents(bean, propertyName, collection, freshValue);
					}
					else if (value instanceof Map map) {
						replaceContents(bean, propertyName, map, freshValue);
					}
					else if (value != null && freshValue != null && isResettableNestedType(value.getClass())
							&& isStableInstance(target, propertyName, value)) {
						resetProperties(value, freshValue, visited, depth + 1);
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
	 * Whether re-reading the given read-only property returns the very same instance. A
	 * getter that instead returns a new object on every call cannot be holding any state
	 * that a reset would need to affect, so recursing into such a value would only ever
	 * walk a throw-away object graph - one that, for record-style/fluent accessors, may
	 * not even terminate (see gh-1750).
	 */
	private boolean isStableInstance(BeanWrapper target, String propertyName, Object value) {
		try {
			return target.getPropertyValue(propertyName) == value;
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
	 * Bring a getter-only collection to the contents of the fresh instance without ever
	 * leaving it empty: nothing is touched when the contents are already equal; a list is
	 * updated element by element, so a reader iterating over it is not disturbed unless
	 * its size changes; any other collection drops only the removed elements before the
	 * new ones are added.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void replaceContents(Object bean, String propertyName, Collection collection, @Nullable Object freshValue) {
		Collection fresh = (freshValue instanceof Collection freshCollection) ? freshCollection
				: Collections.emptyList();
		if (collection.equals(fresh)) {
			return;
		}
		try {
			if (collection instanceof List list) {
				replaceElements(list, fresh);
			}
			else {
				collection.retainAll(fresh);
				collection.addAll(fresh);
			}
		}
		catch (UnsupportedOperationException ex) {
			warnCannotReset(bean, propertyName, ex);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void replaceElements(List list, Collection fresh) {
		int index = 0;
		Iterator iterator = fresh.iterator();
		while (iterator.hasNext() && index < list.size()) {
			Object element = iterator.next();
			if (!ObjectUtils.nullSafeEquals(list.get(index), element)) {
				list.set(index, element);
			}
			index++;
		}
		if (iterator.hasNext()) {
			while (iterator.hasNext()) {
				list.add(iterator.next());
			}
		}
		else if (index < list.size()) {
			list.subList(index, list.size()).clear();
		}
	}

	/**
	 * Bring a getter-only map to the contents of the fresh instance without ever leaving
	 * it empty: nothing is touched when the contents are already equal, and otherwise
	 * only the removed keys are dropped before the fresh entries are put.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void replaceContents(Object bean, String propertyName, Map map, @Nullable Object freshValue) {
		Map fresh = (freshValue instanceof Map freshMap) ? freshMap : Collections.emptyMap();
		if (map.equals(fresh)) {
			return;
		}
		try {
			map.keySet().retainAll(fresh.keySet());
			map.putAll(fresh);
		}
		catch (UnsupportedOperationException ex) {
			warnCannotReset(bean, propertyName, ex);
		}
	}

	private static boolean hasField(Object bean, String propertyName) {
		return ReflectionUtils.findField(AopUtils.getTargetClass(bean), propertyName) != null;
	}

	private static @Nullable Object readProperty(BeanWrapper wrapper, Object instance, String propertyName) {
		if (wrapper.isReadableProperty(propertyName)) {
			return wrapper.getPropertyValue(propertyName);
		}
		Field field = ReflectionUtils.findField(AopUtils.getTargetClass(instance), propertyName);
		if (field == null) {
			return null;
		}
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, instance);
	}

	/**
	 * Whether the given property is a dependency injection point (an annotated setter or
	 * backing field, for example {@code @Autowired} or {@code @Value}) rather than a
	 * bound property.
	 */
	private boolean isInjected(Object bean, PropertyDescriptor pd) {
		Method writeMethod = pd.getWriteMethod();
		if (writeMethod != null && isInjected(writeMethod.getAnnotations())) {
			return true;
		}
		Field field = ReflectionUtils.findField(AopUtils.getTargetClass(bean), pd.getName());
		return field != null && isInjected(field.getAnnotations());
	}

	private static boolean isInjected(Annotation[] annotations) {
		for (Annotation annotation : annotations) {
			if (INJECTION_ANNOTATIONS.contains(annotation.annotationType().getName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Log, at most once per bean type and property, that a property could not be reset to
	 * its freshly bound value and may therefore retain a stale value after this and
	 * future refreshes. The full exception is only logged at DEBUG, on every occurrence,
	 * to avoid spamming the log at the default level while still making the failure
	 * discoverable.
	 */
	private void warnCannotReset(Object bean, String propertyName, Exception ex) {
		String targetClassName = AopUtils.getTargetClass(bean).getName();
		if (this.loggedResetFailures.add(targetClassName + "#" + propertyName)) {
			logger.warn("Cannot reset property '" + propertyName + "' on " + targetClassName
					+ " to its default value; it may retain its previous value after this and future refreshes. "
					+ "Enable DEBUG logging for " + ConfigurationPropertiesRebinder.class.getName()
					+ " for the full exception.");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Failed to reset property '" + propertyName + "' on " + targetClassName, ex);
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
		return !isNeverReset(type);
	}

	/**
	 * Whether the given value was excluded through the
	 * {@code spring.cloud.refresh.never-reset-nested-types} property.
	 */
	private boolean isNeverReset(Object value) {
		return value != null && isNeverReset(value.getClass());
	}

	/**
	 * Whether the given type was excluded through the
	 * {@code spring.cloud.refresh.never-reset-nested-types} property. The exclusion is
	 * matched against a property's declared type as well as the runtime class of its
	 * value, so that a type the user asked us to leave alone is neither descended into
	 * nor overwritten with the default instance's value, whether or not the value happens
	 * to be an instance of a subtype whose own name does not carry the excluded prefix.
	 */
	private boolean isNeverReset(Class<?> type) {
		if (type == null) {
			return false;
		}
		String typeName = type.getName();
		for (String excluded : this.neverResetNestedTypes) {
			if (typeName.startsWith(excluded)) {
				return true;
			}
		}
		return false;
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
