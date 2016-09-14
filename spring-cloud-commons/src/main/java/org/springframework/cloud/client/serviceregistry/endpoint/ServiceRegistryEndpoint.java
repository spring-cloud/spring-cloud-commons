/*
 * Copyright 2013-2016 the original author or authors.
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
 *
 */

package org.springframework.cloud.client.serviceregistry.endpoint;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Endpoint to display and set the service instance status using the service registry.
 *
 * @author Spencer Gibb
 */
@ManagedResource(description = "Can be used to display and set the service instance status using the service registry")
@SuppressWarnings("unchecked")
public class ServiceRegistryEndpoint implements MvcEndpoint {

	private final ServiceRegistry serviceRegistry;

	private Registration registration;

	public ServiceRegistryEndpoint(ServiceRegistry<?> serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setRegistration(Registration registration) {
		this.registration = registration;
	}

	@RequestMapping(path = "instance-status", method = RequestMethod.POST)
	@ResponseBody
	@ManagedOperation
	public ResponseEntity<?> setStatus(@RequestBody String status) {
		Assert.notNull(status, "status may not by null");

		if (this.registration == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no registration found");
		}

		this.serviceRegistry.setStatus(this.registration, status);
		// getEurekaClient().setStatus(status.getStatus());
		// getEurekaClient().cancelOverrideStatus();
		return ResponseEntity.ok().build();
	}

	@RequestMapping(path = "instance-status", method = RequestMethod.GET)
	@ResponseBody
	@ManagedAttribute
	public ResponseEntity getStatus() {
		if (this.registration == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("no registration found");
		}

		// return new Status(this.infoManager.getInfo().getStatus(), this.infoManager.getInfo().getOverriddenStatus());
		return ResponseEntity.ok().body(this.serviceRegistry.getStatus(this.registration));
	}

	@Override
	public String getPath() {
		return "/service-registry";
	}

	@Override
	public boolean isSensitive() {
		return true;
	}

	@Override
	public Class<? extends Endpoint<?>> getEndpointType() {
		return null;
	}
}
