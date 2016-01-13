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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.client.Traverson;
import org.springframework.util.Assert;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

/**
 * A REST resource that is defined by a service reference and a traversal operation within that service.
 *
 * @author Oliver Gierke
 */
@Slf4j
@RequiredArgsConstructor
public class DiscoveredResource implements RemoteResource {

	private final ServiceInstanceProvider provider;
	private final TraversalDefinition traversal;

	private RestOperations restOperations = new RestTemplate();
	private @Getter Link link = null;

	/**
	 * Configures the {@link RestOperations} to use to execute the traversal and verifying HEAD calls.
	 * 
	 * @param restOperations can be {@literal null}, resorting to a default {@link RestTemplate} in that case.
	 */
	public void setRestOperations(RestOperations restOperations) {
		this.restOperations = restOperations == null ? new RestTemplate() : restOperations;
	}

	/**
	 * Verifies the link to the current
	 */
	public void verifyOrDiscover() {
		this.link = link == null ? discoverLink() : verify(link);
	}

	/**
	 * Verifies the given {@link Link} by issuing an HTTP HEAD request to the resource.
	 * 
	 * @param link must not be {@literal null}.
	 * @return
	 */
	private Link verify(Link link) {

		Assert.notNull(link, "Link must not be null!");

		try {

			String uri = link.expand().getHref();

			log.debug("Verifying link pointing to {}…", uri);
			restOperations.headForHeaders(uri);
			log.debug("Successfully verified link!");

			return link;

		} catch (RestClientException o_O) {

			log.debug("Verification failed, marking as outdated!");
			return null;
		}
	}

	private Link discoverLink() {

		try {

			ServiceInstance service = provider.getServiceInstance();

			if (service == null) {
				return null;
			}

			URI uri = service.getUri();
			String serviceId = service.getServiceId();

			log.debug("Discovered {} system at {}. Discovering resource…", serviceId, uri);

			Traverson traverson = new Traverson(uri, MediaTypes.HAL_JSON);
			Link link = traversal.buildTraversal(traverson).asTemplatedLink();

			log.debug("Found link pointing to {}.", link.getHref());

			return link;

		} catch (RuntimeException o_O) {

			this.link = null;
			log.debug("Target system unavailable. Got: ", o_O.getMessage());

			return null;
		}
	}
}
