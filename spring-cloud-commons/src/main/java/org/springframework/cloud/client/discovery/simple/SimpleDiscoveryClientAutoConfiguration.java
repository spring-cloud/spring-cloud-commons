/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

// import org.springframework.boot.context.embedded.EmbeddedServletContainer;
// import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;

/**
 * Spring Boot auto-configuration for simple properties-based discovery client.
 *
 * @author Biju Kunjummen
 */

@Configuration
@AutoConfigureBefore(NoopDiscoveryClientAutoConfiguration.class)
public class SimpleDiscoveryClientAutoConfiguration {

	@Autowired(required = false)
	private ServerProperties server;

	@Autowired
	private ApplicationContext context;

	@Value("${spring.application.name:application}")
	private String serviceId;

	@Autowired
	private InetUtils inet;

	@Bean
	public SimpleDiscoveryProperties simpleDiscoveryProperties() {
		SimpleDiscoveryProperties simple = new SimpleDiscoveryProperties();
		simple.getLocal().setServiceId(this.serviceId);
		simple.getLocal()
				.setUri(URI.create(
						"http://" + this.inet.findFirstNonLoopbackHostInfo().getHostname()
								+ ":" + findPort()));
		return simple;
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public DiscoveryClient simpleDiscoveryClient() {
		return new SimpleDiscoveryClient(simpleDiscoveryProperties());
	}

	private int findPort() {
		// FIXME: is what is the boot 2.0 equiv?
		/*
		 * if (ClassUtils.isPresent(
		 * "org.springframework.boot.context.embedded.EmbeddedWebApplicationContext",
		 * null)) { if (this.context instanceof EmbeddedWebApplicationContext) {
		 * EmbeddedServletContainer container = ((EmbeddedWebApplicationContext)
		 * this.context) .getEmbeddedServletContainer(); if (container != null) { return
		 * container.getPort(); } } }
		 */
		if (this.server != null && this.server.getPort() != null
				&& this.server.getPort() > 0) {
			return this.server.getPort();
		}
		return 8080;
	}

}
