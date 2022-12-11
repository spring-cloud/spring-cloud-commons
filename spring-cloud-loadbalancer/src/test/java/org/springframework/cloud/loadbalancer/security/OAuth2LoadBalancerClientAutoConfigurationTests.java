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

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.test.ClassPathExclusions;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author Dave Syer
 *
 */
@ClassPathExclusions("spring-retry-*.jar")
public class OAuth2LoadBalancerClientAutoConfigurationTests {

	private ConfigurableApplicationContext context;

	@BeforeEach
	public void before() {
		// FIXME: why do I need to do this? (fails in maven build without it.
		// https://stackoverflow.com/questions/28911560/tomcat-8-embedded-error-org-apache-catalina-core-containerbase-a-child-con
		// https://github.com/spring-projects/spring-boot/issues/21535
		TomcatURLStreamHandlerFactory.disable();
	}

	@AfterEach
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@EnableAutoConfiguration
	@Configuration(proxyBeanMethods = false)
	// @EnableOAuth2Sso
	protected static class ClientConfiguration {

	}

}
