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

package org.springframework.cloud.client.discovery.noop;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;

/**
 * @deprecated Use
 * {@link org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration
 * instead}.
 * @author Dave Syer
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties
@ConditionalOnMissingBean(DiscoveryClient.class)
@Deprecated
public class NoopDiscoveryClientAutoConfiguration
		implements ApplicationListener<ContextRefreshedEvent> {

	private final Log log = LogFactory.getLog(NoopDiscoveryClientAutoConfiguration.class);

	@Autowired(required = false)
	private ServerProperties server;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private Environment environment;

	@Autowired(required = false)
	private PortFinder portFinder;

	private DefaultServiceInstance serviceInstance;

	@PostConstruct
	public void init() {
		String host = "localhost";
		try {
			host = InetAddress.getLocalHost().getHostName();
		}
		catch (UnknownHostException e) {
			this.log.warn("Cannot get host info: (" + e.getMessage() + ")");
		}
		int port = findPort();
		this.serviceInstance = new DefaultServiceInstance(
				this.environment.getProperty("spring.application.name", "application"),
				host, port, false);
	}

	private int findPort() {
		int port = 0;
		if (this.server != null && this.server.getPort() != null) {
			port = this.server.getPort();
		}
		if (port != 0 && this.portFinder != null) {
			Integer found = this.portFinder.findPort();
			if (found != null) {
				port = found;
			}
		}
		else {
			// Apparently spring-web is not on the classpath
			if (this.log.isDebugEnabled()) {
				this.log.debug(
						"Could not locate port in embedded container (spring-web not available)");
			}
		}
		return port;
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		this.context.publishEvent(new InstanceRegisteredEvent<>(this, this.environment));
	}

	@Bean
	public DiscoveryClient discoveryClient() {
		return new NoopDiscoveryClient(this.serviceInstance);
	}

	private interface PortFinder {

		Integer findPort();

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = {
			"org.springframework.web.context.support.GenericWebApplicationContext",
			"org.springframework.boot.context.embedded.EmbeddedWebApplicationContext" })
	protected static class Boot15PortFinderConfiguration {

		@Bean
		public PortFinder portFinder(final ApplicationContext context) {
			return new PortFinder() {
				@Override
				public Integer findPort() {
					// TODO: support reactive
					/*
					 * if (context instanceof EmbeddedWebApplicationContext) {
					 * EmbeddedServletContainer container =
					 * ((EmbeddedWebApplicationContext) context)
					 * .getEmbeddedServletContainer(); if (container != null) { return
					 * container.getPort(); } }
					 */
					return null;
				}
			};
		}

	}

}
