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

package org.springframework.cloud.client.discovery.simple.reactive;

import java.net.URI;

import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for mapping properties to instances in {@link SimpleDiscoveryClient}
 *
 * @author Biju Kunjummen
 */

@RunWith(SpringRunner.class)
@SpringBootTest(properties = { "spring.application.name=service0",
		"spring.cloud.discovery.client.simple.instances.service1[0].uri=http://s11:8080",
		"spring.cloud.discovery.client.simple.instances.service1[1].uri=https://s12:8443",
		"spring.cloud.discovery.client.simple.instances.service2[0].uri=https://s21:8080",
		"spring.cloud.discovery.client.simple.instances.service2[1].uri=https://s22:443" })
public class SimpleReactiveDiscoveryClientPropertiesMappingTests {

	@Autowired
	private SimpleReactiveDiscoveryProperties props;

	@Autowired
	private SimpleReactiveDiscoveryClient discoveryClient;

	@Test
	public void propsShouldGetCleanlyMapped() {
		then(this.props.getInstances().size()).isEqualTo(2);
		then(this.props.getInstances().get("service1").size()).isEqualTo(2);
		then(this.props.getInstances().get("service1").get(0).getHost()).isEqualTo("s11");
		then(this.props.getInstances().get("service1").get(0).getPort()).isEqualTo(8080);
		then(this.props.getInstances().get("service1").get(0).getUri())
				.isEqualTo(URI.create("http://s11:8080"));
		then(this.props.getInstances().get("service1").get(0).isSecure())
				.isEqualTo(false);

		then(this.props.getInstances().get("service2").size()).isEqualTo(2);
		then(this.props.getInstances().get("service2").get(0).getHost()).isEqualTo("s21");
		then(this.props.getInstances().get("service2").get(0).getPort()).isEqualTo(8080);
		then(this.props.getInstances().get("service2").get(0).getUri())
				.isEqualTo(URI.create("https://s21:8080"));
		then(this.props.getInstances().get("service2").get(0).isSecure()).isEqualTo(true);
	}

	@Test
	public void testDiscoveryClientShouldResolveSimpleValues() {
		then(this.discoveryClient.description())
				.isEqualTo("Simple Reactive Discovery Client");

		Flux<ServiceInstance> services = this.discoveryClient.getInstances("service1");
		StepVerifier.create(services)
				.expectNextMatches(
						inst -> inst.getHost().equals("s11") && inst.getPort() == 8080
								&& inst.getUri().toString().equals("http://s11:8080")
								&& !inst.isSecure() && inst.getScheme().equals("http"))
				.expectNextMatches(
						inst -> inst.getHost().equals("s12") && inst.getPort() == 8443
								&& inst.getUri().toString().equals("https://s12:8443")
								&& inst.isSecure() && inst.getScheme().equals("https"))
				.expectComplete().verify();
	}

	@Test
	public void testGetANonExistentServiceShouldReturnAnEmptyList() {
		then(this.discoveryClient.description())
				.isEqualTo("Simple Reactive Discovery Client");

		Flux<ServiceInstance> services = this.discoveryClient.getInstances("nonexistent");

		StepVerifier.create(services).expectNextCount(0).expectComplete().verify();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	public static class SampleConfig {

	}

}
