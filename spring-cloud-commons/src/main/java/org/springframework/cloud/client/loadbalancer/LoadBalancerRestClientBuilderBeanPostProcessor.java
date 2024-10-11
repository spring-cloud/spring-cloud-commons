/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.cloud.client.loadbalancer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * {@link RestClient.Builder}-specific
 * {@link AbstractLoadBalancerBlockingBuilderBeanPostProcessor} implementation. Adds the
 * provided {@link ClientHttpRequestInterceptor} to all {@link RestClient.Builder}
 * instances annotated with {@link LoadBalanced}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.1.0
 */
public class LoadBalancerRestClientBuilderBeanPostProcessor<T extends ClientHttpRequestInterceptor>
		extends AbstractLoadBalancerBlockingBuilderBeanPostProcessor<T> {

	/**
	 * Creates a {@link LoadBalancerRestClientBuilderBeanPostProcessor} instance using a
	 * provided {@link ClientHttpRequestInterceptor} and application context.
	 * @param loadBalancerInterceptor a {@link ClientHttpRequestInterceptor} used for
	 * load-balancing
	 * @param context {@link ApplicationContext}
	 * @deprecated in favour of
	 * {@link LoadBalancerRestClientBuilderBeanPostProcessor#LoadBalancerRestClientBuilderBeanPostProcessor(ObjectProvider, ApplicationContext)}
	 */
	@Deprecated(forRemoval = true)
	public LoadBalancerRestClientBuilderBeanPostProcessor(T loadBalancerInterceptor, ApplicationContext context) {
		this(new SimpleObjectProvider<>(loadBalancerInterceptor), context);
	}

	/**
	 * Creates a {@link LoadBalancerRestClientBuilderBeanPostProcessor} instance using
	 * interceptor {@link ObjectProvider} and application context.
	 * @param loadBalancerInterceptorProvider an {@link ObjectProvider} for
	 * {@link ClientHttpRequestInterceptor} used for load-balancing
	 * @param context {@link ApplicationContext}
	 */
	public LoadBalancerRestClientBuilderBeanPostProcessor(ObjectProvider<T> loadBalancerInterceptorProvider,
			ApplicationContext context) {
		super(loadBalancerInterceptorProvider, context);
	}

	@Override
	protected Object apply(Object bean, ClientHttpRequestInterceptor interceptor) {
		return ((RestClient.Builder) bean).requestInterceptor(interceptor);
	}

	@Override
	protected boolean isSupported(Object bean) {
		return bean instanceof RestClient.Builder;
	}

}
