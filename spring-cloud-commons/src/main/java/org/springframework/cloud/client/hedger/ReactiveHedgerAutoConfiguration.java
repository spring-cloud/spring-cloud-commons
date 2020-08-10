/*
 * Copyright 2013-2020 the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * @author Kevin Binswanger
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(WebClient.class)
@ConditionalOnBean(HedgerPolicyFactory.class)
public class ReactiveHedgerAutoConfiguration {
	@Bean
	HedgerExchangeFilterFunction hedgingExchangeFilterFunction(HedgerPolicyFactory hedgerPolicyFactory) {
		return new HedgerExchangeFilterFunction(hedgerPolicyFactory);
	}

	@Bean
	@ConditionalOnMissingBean(HedgedWebClientBuilderBeanPostProcessor.class)
	HedgedWebClientBuilderBeanPostProcessor hedgingWebClientBuilderPostProcessor(
			HedgerExchangeFilterFunction exchangeFilterFunction,
			ApplicationContext context
	) {
		return new HedgedWebClientBuilderBeanPostProcessor(exchangeFilterFunction, context);
	}
}
