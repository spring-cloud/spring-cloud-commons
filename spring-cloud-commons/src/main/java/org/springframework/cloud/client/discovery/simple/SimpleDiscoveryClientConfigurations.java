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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * {@link Configuration Configurations} for import into
 * {@link SimpleDiscoveryClientAutoConfiguration}.
 *
 * @author Biju Kunjummen
 * @author Charu Covindane
 * @author Andy Wilkinson
 */
class SimpleDiscoveryClientConfigurations {

	static abstract class SimpleDiscoveryClientConfiguration {

		protected final SimpleDiscoveryProperties simple = new SimpleDiscoveryProperties();

		protected final InetUtils inet;

		SimpleDiscoveryClientConfiguration(InetUtils inet) {
			this.inet = inet;
		}

		@Bean
		@Order
		public DiscoveryClient simpleDiscoveryClient(SimpleDiscoveryProperties properties) {
			return new SimpleDiscoveryClient(properties);
		}

		@Bean
		@ConditionalOnMissingBean
		public SimpleDiscoveryProperties simpleDiscoveryProperties(
				@Value("${spring.application.name:application}") String serviceId) {
			this.simple.getLocal().setServiceId(serviceId);
			this.simple.getLocal().setHost(this.inet.findFirstNonLoopbackHostInfo().getHostname());
			this.simple.getLocal().setPort(findPort());
			return this.simple;
		}

		protected int findPort() {
			return 8080;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingClass("org.springframework.boot.web.server.context.WebServerInitializedEvent")
	static class StandardSimpleDiscoveryClientConfiguration extends SimpleDiscoveryClientConfiguration {

		StandardSimpleDiscoveryClientConfiguration(InetUtils inet) {
			super(inet);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(WebServerInitializedEvent.class)
	static class WebApplicationSimpleDiscoveryClientConfiguration extends SimpleDiscoveryClientConfiguration
			implements ApplicationListener<WebServerInitializedEvent> {

		private final ObjectProvider<ServerProperties> serverProperties;

		private int port;

		WebApplicationSimpleDiscoveryClientConfiguration(InetUtils inet,
				ObjectProvider<ServerProperties> serverProperties) {
			super(inet);
			this.serverProperties = serverProperties;
		}

		@Override
		public void onApplicationEvent(WebServerInitializedEvent webServerInitializedEvent) {
			this.port = webServerInitializedEvent.getWebServer().getPort();
			if (this.port > 0) {
				this.simple.getLocal().setHost(this.inet.findFirstNonLoopbackHostInfo().getHostname());
				this.simple.getLocal().setPort(this.port);
			}
		}

		@Override
		protected int findPort() {
			if (this.port > 0) {
				return this.port;
			}
			ServerProperties server = this.serverProperties.getIfAvailable();
			if (server != null && server.getPort() != null && server.getPort() > 0) {
				return server.getPort();
			}
			return super.findPort();
		}

	}

}
