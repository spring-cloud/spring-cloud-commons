/*
 * Copyright 2013-2025 the original author or authors.
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

import org.springframework.web.service.invoker.HttpRequestValues;

/**
 * @author Olga Maciaszek-Sharma
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

	@Override
	public void process(Method method, @Nullable Object[] arguments, HttpRequestValues.Builder builder) {
		builder.addAttribute(METHOD_ATTRIBUTE_NAME, method.getName());
		builder.addAttribute(PARAMETER_TYPES_ATTRIBUTE_NAME, method.getParameterTypes());
		builder.addAttribute(ARGUMENTS_ATTRIBUTE_NAME, arguments);
	}

}
