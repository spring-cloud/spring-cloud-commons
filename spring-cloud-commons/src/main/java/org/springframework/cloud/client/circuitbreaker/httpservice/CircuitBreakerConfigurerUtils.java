package org.springframework.cloud.client.circuitbreaker.httpservice;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;

import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.web.service.invoker.HttpRequestValues;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CircuitBreakerConfigurerUtils {

	private static final Log LOG = LogFactory.getLog(CircuitBreakerConfigurerUtils.class);

	static Class<?> resolveFallbackClass(String className) {
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

	static Method resolveFallbackMethod(Map<String, Object> attributes, boolean withThrowable, Class<?> fallbackClass) {
		if (fallbackClass == null) {
			return null;
		}
		String methodName = String.valueOf(attributes.get(CircuitBreakerRequestValueProcessor.METHOD_ATTRIBUTE_NAME));
		Class<?>[] paramTypes = (Class<?>[]) attributes
				.get(CircuitBreakerRequestValueProcessor.PARAMETER_TYPES_ATTRIBUTE_NAME);
		paramTypes = paramTypes != null ? paramTypes : new Class<?>[0];
		Class<?>[] effectiveTypes = withThrowable
				? Stream.concat(Stream.of(Throwable.class), Arrays.stream(paramTypes))
				.toArray(Class[]::new)
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
					? Stream.concat(Stream.of(throwable), Arrays.stream(args))
					.toArray(Object[]::new) : args;
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

	static Object getFallback(HttpRequestValues requestValues, Throwable throwable,
			Object fallbackProxy, Class<?> fallbackClass) {
		Map<String, Object> attributes = requestValues.getAttributes();
		Method fallback = resolveFallbackMethod(attributes, false, fallbackClass);
		Method fallbackWithCause = resolveFallbackMethod(attributes, true, fallbackClass);
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

}
