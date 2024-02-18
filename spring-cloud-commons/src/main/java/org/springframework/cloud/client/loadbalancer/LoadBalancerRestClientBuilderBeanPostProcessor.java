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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * A {@link BeanPostProcessor} that adds the provided {@link ClientHttpRequestInterceptor}
 * to all {@link RestClient.Builder} instances annotated with {@link LoadBalanced}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.1.0
 * @deprecated to be removed in the next release.
 */
@Deprecated(forRemoval = true)
public class LoadBalancerRestClientBuilderBeanPostProcessor implements BeanPostProcessor {

	private final ClientHttpRequestInterceptor loadBalancerInterceptor;

	private final ApplicationContext context;

	public LoadBalancerRestClientBuilderBeanPostProcessor(ClientHttpRequestInterceptor loadBalancerInterceptor,
			ApplicationContext context) {
		this.loadBalancerInterceptor = loadBalancerInterceptor;
		this.context = context;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof RestClient.Builder) {
			if (context.findAnnotationOnBean(beanName, LoadBalanced.class) == null) {
				return bean;
			}
			((RestClient.Builder) bean).requestInterceptor(loadBalancerInterceptor);
		}
		return bean;
	}

}
