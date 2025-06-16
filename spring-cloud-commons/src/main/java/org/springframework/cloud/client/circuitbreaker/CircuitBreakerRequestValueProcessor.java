package org.springframework.cloud.client.circuitbreaker;

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.web.service.invoker.HttpRequestValues;

/**
 * @author Olga Maciaszek-Sharma
 */
public class CircuitBreakerRequestValueProcessor implements HttpRequestValues.Processor {

	public static final String METHOD_ATTRIBUTE_NAME = "spring.cloud.method.name";
	public static final String PARAMETER_TYPES_ATTRIBUTE_NAME = "spring.cloud.method.parameter-types";
	public static final String ARGUMENTS_ATTRIBUTE_NAME = "spring.cloud.method.arguments";

	@Override
	public void process(Method method, @Nullable Object[] arguments, HttpRequestValues.Builder builder) {
		builder.addAttribute(METHOD_ATTRIBUTE_NAME, method.getName());
		builder.addAttribute(PARAMETER_TYPES_ATTRIBUTE_NAME, method.getParameterTypes());
		builder.addAttribute(ARGUMENTS_ATTRIBUTE_NAME, arguments);
	}
}
