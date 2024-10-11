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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;

/**
 * A {@link BeanPostProcessor} that adds the provided {@link ClientHttpRequestInterceptor}
 * to bean instances annotated with {@link LoadBalanced}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.2.0
 */
public abstract class AbstractLoadBalancerBlockingBuilderBeanPostProcessor<T extends ClientHttpRequestInterceptor>
		implements BeanPostProcessor {

	protected final ObjectProvider<T> loadBalancerInterceptorProvider;

	protected final ApplicationContext context;

	AbstractLoadBalancerBlockingBuilderBeanPostProcessor(ObjectProvider<T> loadBalancerInterceptorProvider,
			ApplicationContext context) {
		this.loadBalancerInterceptorProvider = loadBalancerInterceptorProvider;
		this.context = context;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		// a separate call to verify supported type before searching for annotation for
		// performance reasons
		if (isSupported(bean)) {
			if (context.findAnnotationOnBean(beanName, LoadBalanced.class) == null) {
				return bean;
			}
			ClientHttpRequestInterceptor interceptor = loadBalancerInterceptorProvider.getIfAvailable();
			if (interceptor == null) {
				throw new IllegalStateException(ClientHttpRequestInterceptor.class.getSimpleName() + " not available.");
			}
			bean = apply(bean, interceptor);
		}
		return bean;
	}

	protected abstract Object apply(Object bean, ClientHttpRequestInterceptor interceptor);

	protected abstract boolean isSupported(Object bean);

}
