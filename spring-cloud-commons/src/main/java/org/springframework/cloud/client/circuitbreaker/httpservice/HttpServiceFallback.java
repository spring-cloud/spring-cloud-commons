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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to declare a fallback implementation for one or more HTTP service
 * interfaces.
 *
 * <p>
 * This annotation is used in conjunction with a circuit-breaker mechanism to specify
 * which fallback class should be used when a service call fails using the
 * {@link HttpServiceFallback#value()} attribute.
 * </p>
 *
 * <p>
 * You can annotate a fallback class with multiple {@code @Fallback} annotations to
 * support different service interfaces or configuration groups.
 *
 * <p>
 * If {@link HttpServiceFallback#group()} is specified, the fallback will apply only to
 * the specified group. Otherwise, it is treated as a default and will be used for all
 * groups that do not have an explicit per-group fallback configured for the associated
 * service interfaces. Per-group fallback configurations always take precedence over
 * default ones.
 * </p>
 * <p>
 * {@code service()} attribute can be used to specify service interfaces that the fallback
 * class should be used for. If none has been specified, it will be used as a default for
 * all the services that don't have a per-service configuration specified.
 * </p>
 *
 * <p>
 * This annotation is repeatable. To declare multiple fallbacks on the same class, simply
 * annotate it multiple times with {@code @Fallback}.
 * </p>
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 * @see CircuitBreakerRestClientHttpServiceGroupConfigurer
 * @see CircuitBreakerWebClientHttpServiceGroupConfigurer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(HttpServiceFallback.Container.class)
public @interface HttpServiceFallback {

	/**
	 * The class that contains fallback methods to be called by
	 * {@link CircuitBreakerAdapterDecorator} or
	 * {@link ReactiveCircuitBreakerAdapterDecorator} in case a fallback is triggered.
	 * <p>
	 * Both the fallback class and the fallback methods must be public.
	 * </p>
	 */
	Class<?> value();

	/**
	 * The optional list of service interfaces this fallback class applies to.
	 * <p>
	 * If omitted, the fallback is treated as a default for all service interfaces within
	 * the applicable group.
	 * </p>
	 * @return the service interfaces this fallback applies to
	 */
	Class<?>[] service() default {};

	/**
	 * The optional name of the Http Service Group this fallback applies to.
	 * <p>
	 * If omitted or blank, the fallback is treated as a default fallback for all groups.
	 * </p>
	 * @return the group name
	 */
	String group() default "";

	/**
	 * Container annotation to allow multiple {@link HttpServiceFallback} declarations on
	 * the same class.
	 */
	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface Container {

		/**
		 * The set of {@link HttpServiceFallback} annotations.
		 * @return array of fallback declarations
		 */
		HttpServiceFallback[] value();

	}

}
