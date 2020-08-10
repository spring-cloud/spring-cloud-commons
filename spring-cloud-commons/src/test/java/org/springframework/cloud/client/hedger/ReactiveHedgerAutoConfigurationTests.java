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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.Mockito.mock;

/**
 * @author Kevin Binswanger
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class ReactiveHedgerAutoConfigurationTests {
	@Autowired
	private WebClient.Builder webClientBuilder;

	@Autowired
	HedgerPolicyFactory hedgerPolicyFactory;

	@Test
	public void webClientBuilderHadFilterApplied() {
		//noinspection unchecked
		List<ExchangeFilterFunction> filters = (List<ExchangeFilterFunction>)
				ReflectionTestUtils.getField(webClientBuilder, "filters");
		then(filters).hasSize(1);
		//noinspection ConstantConditions
		then(filters.get(0))
				.isInstanceOf(HedgerExchangeFilterFunction.class);
		HedgerExchangeFilterFunction filter =
				(HedgerExchangeFilterFunction) filters.get(0);

		HedgerPolicyFactory actualHedgerPolicyFactory = (HedgerPolicyFactory) ReflectionTestUtils.getField(filter, "hedgingPolicyFactory");
		then(actualHedgerPolicyFactory).isEqualTo(hedgerPolicyFactory);
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	public static class Config {

		@Bean
		HedgerPolicy mockHedgingPolicy() {
			return mock(HedgerPolicy.class);
		}

		@Bean
		HedgerListener reporterA() {
			return mock(HedgerListener.class);
		}

		@Bean
		HedgerListener reporterB() {
			return mock(HedgerListener.class);
		}

		@Bean
		HedgerPolicyFactory hedgingPolicyFactory(HedgerPolicy hedgerPolicy, HedgerListener reporterA, HedgerListener reporterB) {
			return new HedgerPolicyFactory() {
				@Override
				public HedgerPolicy getHedgingPolicy(ClientRequest request) {
					return hedgerPolicy;
				}

				@Override
				public HedgerListener[] getHedgingListeners(ClientRequest request) {
					return new HedgerListener[] {reporterA, reporterB};
				}
			};
		}

		@Bean
		@Hedged
		WebClient.Builder buildWebClient() {
			return WebClient.builder();
		}
	}
}
