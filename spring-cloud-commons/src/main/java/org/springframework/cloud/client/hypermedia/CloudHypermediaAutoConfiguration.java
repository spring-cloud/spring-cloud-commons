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

package org.springframework.cloud.client.hypermedia;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.hypermedia.CloudHypermediaAutoConfiguration.CloudHypermediaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Link;

/**
 * Registers a default {@link RemoteResourceRefresher} if at least one
 * {@link RemoteResource} is declared in the system. Applies verification timings defined
 * in the application properties.
 *
 * @author Oliver Gierke
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Link.class)
@ConditionalOnBean(type = "org.springframework.cloud.client.hypermedia.RemoteResource")
@EnableConfigurationProperties(CloudHypermediaProperties.class)
public class CloudHypermediaAutoConfiguration {

	@Autowired(required = false)
	List<RemoteResource> discoveredResources = Collections.emptyList();

	@Autowired
	CloudHypermediaProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public RemoteResourceRefresher discoveredResourceRefresher() {
		return new RemoteResourceRefresher(this.discoveredResources, this.properties.getRefresh().getFixedDelay(),
				this.properties.getRefresh().getInitialDelay());
	}

	/**
	 * Configuration for Cloud hypermedia.
	 */
	@ConfigurationProperties(prefix = "spring.cloud.hypermedia")
	public static class CloudHypermediaProperties {

		private Refresh refresh = new Refresh();

		public Refresh getRefresh() {
			return this.refresh;
		}

		public void setRefresh(Refresh refresh) {
			this.refresh = refresh;
		}

		/**
		 * Configuration for Cloud hypermedia refresh.
		 */
		public static class Refresh {

			private int fixedDelay = 5000;

			private int initialDelay = 10000;

			public int getFixedDelay() {
				return this.fixedDelay;
			}

			public void setFixedDelay(int fixedDelay) {
				this.fixedDelay = fixedDelay;
			}

			public int getInitialDelay() {
				return this.initialDelay;
			}

			public void setInitialDelay(int initialDelay) {
				this.initialDelay = initialDelay;
			}

		}

	}

}
