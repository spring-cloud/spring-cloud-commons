/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.cloud.client.hedger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * A {@link BeanPostProcessor} that applies
 * {@link HedgerExchangeFilterFunction} filter to all
 * {@link WebClient.Builder} instances annotated with {@link Hedged}.
 *
 * @author Kevin Binswanger
 */
public class HedgedWebClientBuilderBeanPostProcessor implements BeanPostProcessor {
	private final HedgerExchangeFilterFunction exchangeFilterFunction;
	private final ApplicationContext context;

	public HedgedWebClientBuilderBeanPostProcessor(
			HedgerExchangeFilterFunction exchangeFilterFunction,
			ApplicationContext context
	) {
		this.exchangeFilterFunction = exchangeFilterFunction;
		this.context = context;
	}

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, String beanName)
			throws BeansException {
		if (bean instanceof WebClient.Builder) {
			if (context.findAnnotationOnBean(beanName, Hedged.class) == null) {
				return bean;
			}
			((WebClient.Builder) bean).filter(exchangeFilterFunction);
		}
		return bean;
	}
}
