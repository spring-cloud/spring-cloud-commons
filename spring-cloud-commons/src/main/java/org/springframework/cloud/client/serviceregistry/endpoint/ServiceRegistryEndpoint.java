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

package org.springframework.cloud.client.serviceregistry.endpoint;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;

/**
 * Endpoint to display and set the service instance status using the ServiceRegistry.
 *
 * @author Spencer Gibb
 */
@SuppressWarnings("unchecked")
@Endpoint(id = "service-registry")
public class ServiceRegistryEndpoint {

	private final ServiceRegistry serviceRegistry;

	private Registration registration;

	public ServiceRegistryEndpoint(ServiceRegistry<?> serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setRegistration(Registration registration) {
		this.registration = registration;
	}

	@WriteOperation
	public ResponseEntity<?> setStatus(String status) {
		Assert.notNull(status, "status may not by null");

		if (this.registration == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body("no registration found");
		}

		this.serviceRegistry.setStatus(this.registration, status);
		return ResponseEntity.ok().build();
	}

	@ReadOperation
	public ResponseEntity getStatus() {
		if (this.registration == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body("no registration found");
		}

		return ResponseEntity.ok()
				.body(this.serviceRegistry.getStatus(this.registration));
	}

}
