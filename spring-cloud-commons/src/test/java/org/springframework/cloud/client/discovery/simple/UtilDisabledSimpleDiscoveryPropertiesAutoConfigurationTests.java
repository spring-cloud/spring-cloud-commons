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

package org.springframework.cloud.client.discovery.simple;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.client.discovery.simple.reactive.SimpleReactiveDiscoveryProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Akhil CH
 */
@SpringBootTest(classes = UtilDisabledSimpleDiscoveryPropertiesAutoConfigurationTests.Config.class,
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = { "spring.cloud.util.enabled=false", "spring.cloud.discovery.client.simple.local.host=myhost" })
class UtilDisabledSimpleDiscoveryPropertiesAutoConfigurationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private SimpleDiscoveryProperties discoveryProperties;

	@Autowired
	private SimpleReactiveDiscoveryProperties reactiveDiscoveryProperties;

	@LocalServerPort
	private int port;

	@Test
	void startsWithoutInetUtils() {
		then(this.context.getBeansOfType(InetUtils.class)).isEmpty();
		then(this.discoveryProperties.getLocal().getHost()).isEqualTo("myhost");
		then(this.discoveryProperties.getLocal().getPort()).isEqualTo(this.port);
		then(this.reactiveDiscoveryProperties.getLocal().getHost()).isEqualTo("myhost");
		then(this.reactiveDiscoveryProperties.getLocal().getPort()).isEqualTo(this.port);
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
