/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.loadbalancer.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

/**
 * Declarative configuration for a load balancer client. Add this annotation to any
 * <code>@Configuration</code> and then inject a {@link LoadBalancerClientFactory} to
 * access the client that is created.
 *
 * @author Dave Syer
 */
@Configuration
@Import(LoadBalancerClientConfigurationRegistrar.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoadBalancerClient {

	/**
	 * Synonym for name (the name of the client).
	 *
	 * @see #name()
	 * @return the name of the load balancer client
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of the load balancer client, uniquely identifying a set of client
	 * resources, including a load balancer.
	 * @return the name of the load balancer client
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * A custom <code>@Configuration</code> for the load balancer client. Can contain
	 * override <code>@Bean</code> definition for the pieces that make up the client.
	 *
	 * @see LoadBalancerClientConfiguration for the defaults
	 * @return configuration classes for the load balancer client.
	 */
	Class<?>[] configuration() default {};

}
