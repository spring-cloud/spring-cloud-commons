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

package org.springframework.cloud.loadbalancer.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClientSpecification;
import org.springframework.cloud.loadbalancer.support.fixture.LoadBalancerClientConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.accept.ApiVersionParser;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for {@link LoadBalancerClientFactory}.
 *
 * @author akenra
 */
class LoadBalancerClientFactoryTests {

	@Test
	void shouldApplyClientConfigurationWithSameSimpleNameAsDefaultConfiguration() {
		LoadBalancerClientFactory factory = new LoadBalancerClientFactory(new LoadBalancerClientsProperties());
		AnnotationConfigApplicationContext parent = new AnnotationConfigApplicationContext();
		parent.getBeanFactory().registerSingleton("loadBalancerClientFactory", factory);
		parent.refresh();
		factory.setApplicationContext(parent);
		factory.setConfigurations(List.of(new LoadBalancerClientSpecification("testservice",
				new Class<?>[] { LoadBalancerClientConfiguration.class })));

		// the client-specific bean must be present despite the configuration class
		// having the same simple name as the default configuration class (gh-1359)
		then(factory.getInstance("testservice", LoadBalancerClientConfiguration.CustomFlag.class)).isNotNull();
		// the default configuration must still be applied
		then(factory.getInstance("testservice", ApiVersionParser.class)).isNotNull();
	}

}
