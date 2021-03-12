/*
 * Copyright 2015-2015 the original author or authors.
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

package org.springframework.cloud.loadbalancer.security;

import java.net.URI;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.EnableOAuth2Sso;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoRestTemplateFactory;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.cloud.test.ModifiedClassPathRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 *
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("spring-retry-*.jar")
public class OAuth2LoadBalancerClientAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Before
	public void before() {
		// FIXME: why do I need to do this? (fails in maven build without it.
		// https://stackoverflow.com/questions/28911560/tomcat-8-embedded-error-org-apache-catalina-core-containerbase-a-child-con
		// https://github.com/spring-projects/spring-boot/issues/21535
		TomcatURLStreamHandlerFactory.disable();
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void userInfoNotLoadBalanced() {
		this.context = new SpringApplicationBuilder(ClientConfiguration.class).properties("spring.config.name=test",
				"server.port=0", "security.oauth2.resource.userInfoUri:https://example.com").run();

		assertThat(this.context.containsBean("loadBalancedUserInfoRestTemplateCustomizer")).isFalse();
		assertThat(this.context.containsBean("retryLoadBalancedUserInfoRestTemplateCustomizer")).isFalse();
	}

	@Test
	public void userInfoLoadBalancedNoRetry() throws Exception {
		this.context = new SpringApplicationBuilder(ClientConfiguration.class).properties("spring.config.name=test",
				"server.port=0", "security.oauth2.resource.userInfoUri:https://nosuchservice",
				"spring.cloud.oauth2.load-balanced.enabled=true").run();

		assertThat(this.context.containsBean("loadBalancedUserInfoRestTemplateCustomizer")).isTrue();
		assertThat(this.context.containsBean("retryLoadBalancedUserInfoRestTemplateCustomizer")).isFalse();

		OAuth2RestTemplate template = this.context.getBean(UserInfoRestTemplateFactory.class).getUserInfoRestTemplate();
		ClientHttpRequest request = template.getRequestFactory().createRequest(new URI("https://nosuchservice"),
				HttpMethod.GET);
		expected.expectMessage("No instances available for nosuchservice");
		request.execute();
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	@EnableOAuth2Sso
	protected static class ClientConfiguration {

	}

}
