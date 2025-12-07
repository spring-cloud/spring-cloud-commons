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

package org.springframework.cloud.client.loadbalancer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * {@link RestTemplateBuilder}-specific
 * {@link AbstractLoadBalancerBlockingBuilderBeanPostProcessor} implementation. Adds the
 * provided {@link ClientHttpRequestInterceptor} to all {@link RestTemplateBuilder}
 * instances annotated with {@link LoadBalanced}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.2.0
 */
public class LoadBalancerRestTemplateBuilderBeanPostProcessor<T extends ClientHttpRequestInterceptor>
		extends AbstractLoadBalancerBlockingBuilderBeanPostProcessor<T> {

	public LoadBalancerRestTemplateBuilderBeanPostProcessor(ObjectProvider<T> loadBalancerInterceptorProvider,
			ApplicationContext context) {
		super(loadBalancerInterceptorProvider, context);
	}

	@Override
	protected boolean isSupported(Object bean) {
		return bean instanceof RestTemplateBuilder;
	}

	@Override
	protected Object apply(Object bean, ClientHttpRequestInterceptor interceptor) {
		return ((RestTemplateBuilder) bean).interceptors(interceptor);
	}

}
