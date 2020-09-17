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

package org.springframework.cloud.client.loadbalancer;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadBalancerUriTools}.
 *
 * @author Olga Maciaszek-Sharma
 */
class LoadBalancerUriToolsTests {

	@Test
	void originalURIReturnedIfDataMatches() {
		TestServiceInstance serviceInstance = new TestServiceInstance();
		URI original = UriComponentsBuilder.fromUriString("http://test.example:8080/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isEqualTo(original);
	}

	@Test
	void serviceInstanceHostSet() {
		TestServiceInstance serviceInstance = new TestServiceInstance();
		URI original = UriComponentsBuilder.fromUriString("http://testHost.example:8080/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getHost()).isEqualTo(serviceInstance.getHost());
	}

	@Test
	void serviceInstanceSchemeSet() {
		TestServiceInstance serviceInstance = new TestServiceInstance().withScheme("https");
		URI original = UriComponentsBuilder.fromUriString("http://test.example/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getScheme()).isEqualTo(serviceInstance.getScheme());
	}

	@Test
	void originalSchemeSetIfServiceInstanceSchemeMissing() {
		TestServiceInstance serviceInstance = new TestServiceInstance().withScheme(null);
		URI original = UriComponentsBuilder.fromUriString("https://test.example/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getScheme()).isEqualTo(original.getScheme());
	}

	@Test
	void secureSchemeSetIfServiceInstanceSchemeMissingAndServiceInstanceSecure() {
		TestServiceInstance serviceInstance = new TestServiceInstance().withScheme(null).withSecure(true);
		URI original = UriComponentsBuilder.fromUriString("http://test.example/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getScheme()).isEqualTo("https");
	}

	@Test
	void secureWsSchemeSetIfServiceInstanceSchemeMissingAndServiceInstanceSecure() {
		TestServiceInstance serviceInstance = new TestServiceInstance().withScheme(null).withSecure(true);
		URI original = UriComponentsBuilder.fromUriString("ws://test.example/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getScheme()).isEqualTo("wss");
	}

	@Test
	void defaultSchemeSetIfMissing() {
		TestServiceInstance serviceInstance = new TestServiceInstance().withScheme(null);
		URI original = UriComponentsBuilder.fromUriString("//test.example/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getScheme()).isEqualTo("http");
	}

	@Test
	void serviceInstancePortSet() {
		TestServiceInstance serviceInstance = new TestServiceInstance().withPort(0);
		URI original = UriComponentsBuilder.fromUriString("http://test.example:8080/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getPort()).isEqualTo(serviceInstance.getPort());
	}

	@Test
	void defaultHttpPortSetIfServiceInstancePortIncorrect() {
		TestServiceInstance serviceInstance = new TestServiceInstance().withPort(-1);
		URI original = UriComponentsBuilder.fromUriString("http://test.example:8888/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getPort()).isEqualTo(80);
	}

	@Test
	void defaultHttpsPortSetIfServiceInstancePortIncorrect() {
		TestServiceInstance serviceInstance = new TestServiceInstance().withScheme("https").withPort(-1);
		URI original = UriComponentsBuilder.fromUriString("http://test.example:8888/xxx").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getPort()).isEqualTo(443);
	}

	@Test
	void originalUserInfoSet() {
		TestServiceInstance serviceInstance = new TestServiceInstance();
		URI original = UriComponentsBuilder
				.fromUriString("http://testUser@testHost.example/path?query1=test1&query2=test2#fragment").build()
				.toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getRawUserInfo()).isEqualTo(original.getRawUserInfo());
		assertThat(reconstructed.getRawQuery()).isEqualTo(original.getRawQuery());
		assertThat(reconstructed.getRawPath()).isEqualTo(original.getRawPath());
		assertThat(reconstructed.getRawQuery()).isEqualTo(original.getRawQuery());
		assertThat(reconstructed.getRawFragment()).isEqualTo(original.getRawFragment());
		assertThat(reconstructed.getHost()).isEqualTo(serviceInstance.getHost());
		assertThat(reconstructed.getPort()).isEqualTo(serviceInstance.getPort());
	}

	@Test
	void reconstructedURIEncodedCorrectly() {
		TestServiceInstance serviceInstance = new TestServiceInstance();
		URI original = UriComponentsBuilder
				.fromUriString("http://test.example/path%40%21%242?query=val%40%21%242#frag%40%21%242").build().toUri();

		URI reconstructed = LoadBalancerUriTools.reconstructURI(serviceInstance, original);

		assertThat(reconstructed).isNotNull();
		assertThat(reconstructed.getRawUserInfo()).isEqualTo(original.getRawUserInfo());
		assertThat(reconstructed.getRawQuery()).isEqualTo(original.getRawQuery());
		assertThat(reconstructed.getRawPath()).isEqualTo(original.getRawPath());
		assertThat(reconstructed.getRawQuery()).isEqualTo(original.getRawQuery());
		assertThat(reconstructed.getRawFragment()).isEqualTo(original.getRawFragment());
		assertThat(reconstructed.getHost()).isEqualTo(serviceInstance.getHost());
		assertThat(reconstructed.getPort()).isEqualTo(serviceInstance.getPort());
	}

}

class TestServiceInstance implements ServiceInstance {

	private URI uri;

	private String scheme = "http";

	private String host = "test.example";

	private int port = 8080;

	private boolean secure;

	private Map<String, String> metadata = new LinkedHashMap<>();

	TestServiceInstance withScheme(String scheme) {
		this.scheme = scheme;
		return this;
	}

	TestServiceInstance withPort(int port) {
		this.port = port;
		return this;
	}

	TestServiceInstance withSecure(boolean secure) {
		this.secure = secure;
		return this;
	}

	@Override
	public String getServiceId() {
		return "test-service";
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public boolean isSecure() {
		return secure;
	}

	@Override
	public URI getUri() {
		return uri;
	}

	@Override
	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public String getScheme() {
		return scheme;
	}

}
