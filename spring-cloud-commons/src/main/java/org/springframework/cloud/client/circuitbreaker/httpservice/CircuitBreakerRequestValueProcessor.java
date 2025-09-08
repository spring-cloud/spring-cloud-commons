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

import java.lang.reflect.Method;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.web.service.invoker.HttpRequestValues;

/**
 * A {@link HttpRequestValues.Processor} that adds information necessary for
 * circuit-breaking to {@link HttpRequestValues}. The following attributes are added to
 * the builder:
 * <ul>
 * <li>{@link #METHOD_ATTRIBUTE_NAME} - The name of the method being invoked.</li>
 * <li>{@link #PARAMETER_TYPES_ATTRIBUTE_NAME} - The types of the parameters of the
 * method.</li>
 * <li>{@link #ARGUMENTS_ATTRIBUTE_NAME} - The actual arguments passed to the method.</li>
 * <li>{@link #RETURN_TYPE_ATTRIBUTE_NAME} - The return type of the method.</li>
 * </ul>
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 */
public class CircuitBreakerRequestValueProcessor implements HttpRequestValues.Processor {

	/**
	 * Spring Cloud-specific attribute name for storing method name.
	 */
	public static final String METHOD_ATTRIBUTE_NAME = "spring.cloud.method.name";

	/**
	 * Spring Cloud-specific attribute name for storing method parameter types.
	 */
	public static final String PARAMETER_TYPES_ATTRIBUTE_NAME = "spring.cloud.method.parameter-types";

	/**
	 * Spring Cloud-specific attribute name for storing method arguments.
	 */
	public static final String ARGUMENTS_ATTRIBUTE_NAME = "spring.cloud.method.arguments";

	/**
	 * Spring Cloud-specific attribute name for storing method return types.
	 */
	public static final String RETURN_TYPE_ATTRIBUTE_NAME = "spring.cloud.method.return-type";

	/**
	 * Spring Cloud-specific attribute name for storing method declaring class name.
	 */
	public static final String DECLARING_CLASS_ATTRIBUTE_NAME = "spring.cloud.method.declaring-class";

	@Override
	public void process(Method method, MethodParameter[] parameters, @Nullable Object[] arguments,
			HttpRequestValues.Builder builder) {
		builder.addAttribute(METHOD_ATTRIBUTE_NAME, method.getName());
		builder.addAttribute(PARAMETER_TYPES_ATTRIBUTE_NAME, method.getParameterTypes());
		builder.addAttribute(ARGUMENTS_ATTRIBUTE_NAME, arguments);
		builder.addAttribute(RETURN_TYPE_ATTRIBUTE_NAME, method.getReturnType());
		builder.addAttribute(DECLARING_CLASS_ATTRIBUTE_NAME, method.getDeclaringClass().getCanonicalName());
	}

}
