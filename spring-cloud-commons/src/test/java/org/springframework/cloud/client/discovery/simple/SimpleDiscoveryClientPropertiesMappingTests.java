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

package org.springframework.cloud.client.discovery.simple;

import java.net.URI;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.BDDAssertions.then;

/**
 * Tests for mapping properties to instances in {@link SimpleDiscoveryClient}.
 *
 * @author Biju Kunjummen
 */

@SpringBootTest(properties = { "spring.application.name=service0",
		"spring.cloud.discovery.client.simple.instances.service1[0].uri=http://s11:8080",
		"spring.cloud.discovery.client.simple.instances.service1[1].uri=https://s12:8443",
		"spring.cloud.discovery.client.simple.instances.service2[0].uri=https://s21:8080",
		"spring.cloud.discovery.client.simple.instances.service2[1].uri=https://s22:443" })
public class SimpleDiscoveryClientPropertiesMappingTests {

	@Autowired
	private SimpleDiscoveryProperties props;

	@Autowired
	private SimpleDiscoveryClient discoveryClient;

	@Test
	public void propsShouldGetCleanlyMapped() {
		then(this.props.getInstances().size()).isEqualTo(2);
		then(this.props.getInstances().get("service1").size()).isEqualTo(2);
		then(this.props.getInstances().get("service1").get(0).getHost()).isEqualTo("s11");
		then(this.props.getInstances().get("service1").get(0).getPort()).isEqualTo(8080);
		then(this.props.getInstances().get("service1").get(0).getUri()).isEqualTo(URI.create("http://s11:8080"));
		then(this.props.getInstances().get("service1").get(0).isSecure()).isEqualTo(false);

		then(this.props.getInstances().get("service2").size()).isEqualTo(2);
		then(this.props.getInstances().get("service2").get(0).getHost()).isEqualTo("s21");
		then(this.props.getInstances().get("service2").get(0).getPort()).isEqualTo(8080);
		then(this.props.getInstances().get("service2").get(0).getUri()).isEqualTo(URI.create("https://s21:8080"));
		then(this.props.getInstances().get("service2").get(0).isSecure()).isEqualTo(true);
	}

	@Test
	public void testDiscoveryClientShouldResolveSimpleValues() {
		then(this.discoveryClient.description()).isEqualTo("Simple Discovery Client");
		then(this.discoveryClient.getInstances("service1")).hasSize(2);

		ServiceInstance s1 = this.discoveryClient.getInstances("service1").get(0);
		then(s1.getHost()).isEqualTo("s11");
		then(s1.getPort()).isEqualTo(8080);
		then(s1.getUri()).isEqualTo(URI.create("http://s11:8080"));
		then(s1.isSecure()).isEqualTo(false);
	}

	@Test
	public void testGetServices() {
		then(this.discoveryClient.getServices()).containsExactlyInAnyOrder("service1", "service2");
	}

	@Test
	public void testGetANonExistentServiceShouldReturnAnEmptyList() {
		then(this.discoveryClient.getInstances("nonexistent")).isNotNull();
		then(this.discoveryClient.getInstances("nonexistent")).isEmpty();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableAutoConfiguration
	public static class SampleConfig {

	}

}
