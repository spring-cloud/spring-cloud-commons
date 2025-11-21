/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.client.circuitbreaker.httpservice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.util.StringUtils;
import org.springframework.web.service.invoker.HttpExchangeAdapterDecorator;
import org.springframework.web.service.invoker.HttpRequestValues;

import static org.springframework.cloud.client.circuitbreaker.httpservice.CircuitBreakerRequestValueProcessor.DECLARING_CLASS_ATTRIBUTE_NAME;

/**
 * Utility class used by CircuitBreaker-specific {@link HttpExchangeAdapterDecorator}
 * implementations.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 */
final class CircuitBreakerConfigurerUtils {

	/**
	 * Default fallback key.
	 */
	public static final String DEFAULT_FALLBACK_KEY = "default";

	private CircuitBreakerConfigurerUtils() {
		throw new UnsupportedOperationException("Cannot instantiate a utility class");
	}

	private static final Log LOG = LogFactory.getLog(CircuitBreakerConfigurerUtils.class);

	static Map<String, Class<?>> resolveFallbackClasses(Map<String, String> fallbackClassNames) {
		return fallbackClassNames.entrySet()
			.stream()
			.collect(Collectors.toMap(java.util.Map.Entry::getKey, entry -> resolveFallbackClass(entry.getValue())));
	}

	@SuppressWarnings("unchecked")
	static <T> T castIfPossible(Object result) {
		try {
			return (T) result;
		}
		catch (ClassCastException exception) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Failed to cast object of type " + result.getClass() + " to expected type.");
			}
			throw exception;
		}
	}

	static Map<String, Class<?>> resolveAnnotatedFallbackClasses(ApplicationContext context,
			@Nullable String groupName) {
		Map<String, Object> fallbackConfigurationBeans = context.getBeansWithAnnotation(HttpServiceFallback.class);
		Map<String, Class<?>> fallbackClasses = new HashMap<>();

		for (Object fallbackConfigurationBean : fallbackConfigurationBeans.values()) {
			MergedAnnotations annotations = MergedAnnotations.from(fallbackConfigurationBean.getClass(),
					MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
			for (MergedAnnotation<HttpServiceFallback> annotation : annotations.stream(HttpServiceFallback.class)
				.toList()) {
				String group = annotation.getString("group");
				if ((StringUtils.hasText(groupName) && groupName.equals(group))
						|| !StringUtils.hasText(groupName) && !StringUtils.hasText(group)) {
					addFallbackEntries(annotation.getClass(MergedAnnotation.VALUE), annotation.getClassArray("service"),
							fallbackClasses);
				}
			}
		}
		return fallbackClasses;
	}

	private static void addFallbackEntries(Class<?> fallbackBeanClass, Class<?>[] services,
			Map<String, Class<?>> fallbackClasses) {
		if (services.length == 0) {
			addFallbackEntry(fallbackClasses, DEFAULT_FALLBACK_KEY, fallbackBeanClass);
		}
		else {
			for (Class<?> serviceClass : services) {
				addFallbackEntry(fallbackClasses, serviceClass.getName(), fallbackBeanClass);
			}
		}
	}

	private static void addFallbackEntry(Map<String, Class<?>> map, String key, Class<?> fallbackClass) {
		if (map.containsKey(key)) {
			throw new IllegalStateException("Duplicate fallback key: " + key);
		}
		map.put(key, fallbackClass);
	}

	static Method resolveFallbackMethod(Map<String, Object> attributes, boolean withThrowable, Class<?> fallbackClass) {
		if (fallbackClass == null) {
			return null;
		}
		String methodName = String.valueOf(attributes.get(CircuitBreakerRequestValueProcessor.METHOD_ATTRIBUTE_NAME));
		Class<?>[] paramTypes = (Class<?>[]) attributes
			.get(CircuitBreakerRequestValueProcessor.PARAMETER_TYPES_ATTRIBUTE_NAME);
		paramTypes = paramTypes != null ? paramTypes : new Class<?>[0];
		Class<?>[] effectiveTypes = withThrowable
				? Stream.concat(Stream.of(Throwable.class), Arrays.stream(paramTypes)).toArray(Class[]::new)
				: paramTypes;

		try {
			Method method = fallbackClass.getMethod(methodName, effectiveTypes);
			method.setAccessible(true);
			return method;
		}
		catch (NoSuchMethodException exception) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Fallback method not found: " + methodName + " in " + fallbackClass.getName(), exception);
			}
			return null;
		}
	}

	static Object invokeFallback(Method method, Map<String, Object> attributes, @Nullable Throwable throwable,
			Object fallbackProxy) {
		try {
			Object[] args = (Object[]) attributes.get(CircuitBreakerRequestValueProcessor.ARGUMENTS_ATTRIBUTE_NAME);
			args = args != null ? args : new Class<?>[0];
			Object[] finalArgs = (throwable != null)
					? Stream.concat(Stream.of(throwable), Arrays.stream(args)).toArray(Object[]::new) : args;
			return method.invoke(fallbackProxy, finalArgs);
		}
		catch (InvocationTargetException | IllegalAccessException exception) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Error invoking fallback method: " + method.getName(), exception);
			}
			Throwable underlyingException = exception.getCause();
			if (underlyingException instanceof RuntimeException) {
				throw (RuntimeException) underlyingException;
			}
			if (underlyingException != null) {
				throw new IllegalStateException("Failed to invoke fallback method", underlyingException);
			}
			throw new RuntimeException("Failed to invoke fallback method", exception);
		}
	}

	static Object getFallback(HttpRequestValues requestValues, Throwable throwable, Map<String, Object> fallbackProxies,
			Map<String, Class<?>> fallbackClasses) {
		Map<String, Object> attributes = requestValues.getAttributes();
		String declaringClassName = (String) attributes.get(DECLARING_CLASS_ATTRIBUTE_NAME);
		Class<?> fallbackClass = fallbackClasses.getOrDefault(declaringClassName,
				fallbackClasses.get(DEFAULT_FALLBACK_KEY));
		Method fallback = resolveFallbackMethod(attributes, false, fallbackClass);
		Method fallbackWithCause = resolveFallbackMethod(attributes, true, fallbackClass);
		Object fallbackProxy = fallbackProxies.getOrDefault(declaringClassName,
				fallbackProxies.get(DEFAULT_FALLBACK_KEY));
		if (fallback != null) {
			return invokeFallback(fallback, attributes, null, fallbackProxy);
		}
		else if (fallbackWithCause != null) {
			return invokeFallback(fallbackWithCause, attributes, throwable, fallbackProxy);
		}
		else {
			throw new NoFallbackAvailableException("No fallback available.", throwable);
		}
	}

	static Map<String, Object> createProxies(Map<String, Class<?>> fallbackClasses) {
		return fallbackClasses.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> createProxy(entry.getValue())));
	}

	private static Class<?> resolveFallbackClass(String className) {
		try {
			return Class.forName(className);
		}
		catch (ClassNotFoundException e) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Fallback class not found: " + className, e);
			}
			throw new IllegalStateException("Unable to load fallback class: " + className, e);
		}
	}

	static Object createProxy(Class<?> fallbackClass) {
		try {
			Object target = fallbackClass.getConstructor().newInstance();
			ProxyFactory proxyFactory = new ProxyFactory(target);
			proxyFactory.setProxyTargetClass(true);
			return proxyFactory.getProxy();
		}
		catch (ReflectiveOperationException exception) {
			if (LOG.isErrorEnabled()) {
				LOG.error("Error instantiating fallback proxy for class: " + fallbackClass.getName() + ", exception: "
						+ exception.getMessage(), exception);
			}
			throw new FallbackProxyCreationException("Could not create fallback proxy", exception);
		}
	}

}
