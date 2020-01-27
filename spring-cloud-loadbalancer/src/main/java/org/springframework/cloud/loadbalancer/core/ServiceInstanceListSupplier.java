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

package org.springframework.cloud.loadbalancer.core;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import reactor.core.publisher.Flux;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;

import static org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory.PROPERTY_NAME;

/**
 * A {@link Supplier} of lists of {@link ServiceInstance} objects.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
public interface ServiceInstanceListSupplier
		extends Supplier<Flux<List<ServiceInstance>>> {

	String getServiceId();

	static FixedServiceInstanceListSupplier.Builder fixed(Environment environment) {
		return new FixedServiceInstanceListSupplier.Builder(environment);
	}

	class FixedServiceInstanceListSupplier implements ServiceInstanceListSupplier {

		private final String serviceId;

		private List<ServiceInstance> instances;

		public static Builder with(Environment env) {
			return new Builder(env);
		}

		private FixedServiceInstanceListSupplier(Environment env,
				List<ServiceInstance> instances) {
			this.serviceId = env.getProperty(PROPERTY_NAME);
			this.instances = instances;
		}

		@Override
		public String getServiceId() {
			return serviceId;
		}

		@Override
		public Flux<List<ServiceInstance>> get() {
			return Flux.just(instances);
		}

		public static final class Builder {

			private final Environment env;

			private final ArrayList<ServiceInstance> instances = new ArrayList<>();

			private Builder(Environment env) {
				this.env = env;
			}

			public Builder instance(ServiceInstance instance) {
				instances.add(instance);
				return this;
			}

			public Builder instance(int port, String serviceId) {
				return instance("localhost", port, serviceId);
			}

			public Builder instance(String host, int port, String serviceId) {
				DefaultServiceInstance instance = new DefaultServiceInstance(
						instanceId(serviceId, host, port), serviceId, host, port, false);
				return instance(instance);
			}

			private String instanceId(String serviceId, String host, int port) {
				return serviceId + ":" + host + ":" + port;
			}

			public FixedServiceInstanceListSupplier build() {
				return new FixedServiceInstanceListSupplier(env, instances);
			}

		}

	}

}
