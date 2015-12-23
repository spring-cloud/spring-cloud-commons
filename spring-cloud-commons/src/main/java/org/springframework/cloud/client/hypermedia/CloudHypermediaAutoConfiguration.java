/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.client.hypermedia;

import lombok.Data;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.hypermedia.CloudHypermediaAutoConfiguration.CloudHypermediaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers a default {@link RemoteResourceRefresher} if at least one {@link RemoteResource} is declared in the system
 * and applies verification timings defined in the application properties.
 * 
 * @author Oliver Gierke
 */
@Configuration
@ConditionalOnBean(type = "org.springframework.cloud.client.hypermedia.RemoteResource")
@EnableConfigurationProperties(CloudHypermediaProperties.class)
public class CloudHypermediaAutoConfiguration {

	@Autowired(required = false) List<RemoteResource> discoveredResources = Collections.emptyList();
	@Autowired CloudHypermediaProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public RemoteResourceRefresher discoveredResourceRefresher() {
		return new RemoteResourceRefresher(discoveredResources, properties.getRefresh().getFixedDelay(),
				properties.getRefresh().getInitialDelay());
	}

	@Data
	@ConfigurationProperties(prefix = "spring.cloud.hypermedia")
	public static class CloudHypermediaProperties {

		private Refresh refresh = new Refresh();

		@Data
		public static class Refresh {

			private int fixedDelay = 5000;
			private int initialDelay = 10000;
		}
	}
}
