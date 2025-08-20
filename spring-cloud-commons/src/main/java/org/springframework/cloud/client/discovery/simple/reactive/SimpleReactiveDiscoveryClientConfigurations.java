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

package org.springframework.cloud.client.discovery.simple.reactive;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * {@link Configuration Configurations} for import into
 * {@link SimpleReactiveDiscoveryClientAutoConfiguration}.
 *
 * @author Tim Ysewyn
 * @author Charu Covindane
 * @author Andy Wilkinson
 */
public class SimpleReactiveDiscoveryClientConfigurations {

	static abstract class SimpleReactiveDiscoveryClientConfiguration {

		protected final SimpleReactiveDiscoveryProperties simple = new SimpleReactiveDiscoveryProperties();

		protected final InetUtils inet;

		SimpleReactiveDiscoveryClientConfiguration(InetUtils inet) {
			this.inet = inet;
		}

		@Bean
		@Order
		public SimpleReactiveDiscoveryClient simpleReactiveDiscoveryClient(
				SimpleReactiveDiscoveryProperties properties) {
			return new SimpleReactiveDiscoveryClient(properties);
		}

		@Bean
		@ConditionalOnMissingBean
		public SimpleReactiveDiscoveryProperties simpleReactiveDiscoveryProperties(
				@Value("${spring.application.name:application}") String serviceId) {
			simple.getLocal().setServiceId(serviceId);
			simple.getLocal().setHost(inet.findFirstNonLoopbackHostInfo().getHostname());
			simple.getLocal().setPort(findPort());
			return simple;
		}

		protected int findPort() {
			return 8080;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.springframework.boot.web.server.context.WebServerInitializedEvent")
	static class StandardSimpleReactiveDiscoveryClientConfiguration extends SimpleReactiveDiscoveryClientConfiguration {

		StandardSimpleReactiveDiscoveryClientConfiguration(InetUtils inet) {
			super(inet);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebServerInitializedEvent.class)
	static class WebApplicationSimpleReactiveDiscoveryClientConfiguration extends
			SimpleReactiveDiscoveryClientConfiguration implements ApplicationListener<WebServerInitializedEvent> {

		private final ObjectProvider<ServerProperties> serverProperties;

		private int port;

		WebApplicationSimpleReactiveDiscoveryClientConfiguration(InetUtils inet,
				ObjectProvider<ServerProperties> serverProperties) {
			super(inet);
			this.serverProperties = serverProperties;
		}

		@Override
		public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
			port = webServerInitializedEvent.getWebServer().getPort();
			if (port > 0) {
				simple.getLocal().setHost(inet.findFirstNonLoopbackHostInfo().getHostname());
				simple.getLocal().setPort(port);
			}
		}

		@Override
		protected int findPort() {
			if (port > 0) {
				return port;
			}
			ServerProperties server = serverProperties.getIfAvailable();
			if (server != null && server.getPort() != null && server.getPort() > 0) {
				return server.getPort();
			}
			return super.findPort();
		}

	}

}
