/*
 * Copyright 2012-2023 the original author or authors.
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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.config.LoadBalancerMultiMainZoneConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An implementation of {@link ServiceInstanceListSupplier} that filters instances
 * retrieved by the delegate by multi main and subset zone.
 * <code>spring.cloud.loadbalancer.multiMainZone.mainZone</code> default DAILY
 * <code>spring.cloud.loadbalancer.multiMainZone.zoneRequestHeaderKey</code> default zone
 *
 * @author seal
 */
public class MultiMainZoneServiceInstanceListSupplier extends DelegatingServiceInstanceListSupplier {


	public static final String MAIN_ZONE = "main-zone";

	public static final String SUBSET_ZONE = "subset-zone";

	private final LoadBalancerMultiMainZoneConfig multiMainZoneConfig;

	private boolean callGetWithRequestOnDelegates;

	public MultiMainZoneServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
													LoadBalancerMultiMainZoneConfig multiMainZoneConfig) {
		super(delegate);
		this.multiMainZoneConfig = multiMainZoneConfig;
	}

	public MultiMainZoneServiceInstanceListSupplier(ServiceInstanceListSupplier delegate,
													LoadBalancerMultiMainZoneConfig multiMainZoneConfig,
													 ReactiveLoadBalancer.Factory<ServiceInstance> loadBalancerClientFactory) {
		super(delegate);
		this.multiMainZoneConfig = multiMainZoneConfig;
		callGetWithRequestOnDelegates = loadBalancerClientFactory.getProperties(getServiceId())
				.isCallGetWithRequestOnDelegates();
	}

	@Override
	public Flux<List<ServiceInstance>> get() {
		return Flux.empty();
	}

	@Override
	public Flux<List<ServiceInstance>> get(Request request) {

		String requestZone = null;
		Object context = request.getContext();
		if ((context instanceof RequestDataContext)) {
			HttpHeaders headers = ((RequestDataContext) context).getClientRequest().getHeaders();
			String zoneHeaderValue = headers.getFirst(this.multiMainZoneConfig.getZoneRequestHeaderKey());
			if(StringUtils.hasText(zoneHeaderValue)) {
				requestZone = zoneHeaderValue;
			}
		}

		String finalRequestZone = requestZone;

		Flux<List<ServiceInstance>> instances;
		if (callGetWithRequestOnDelegates) {
			instances = getDelegate().get(request);
		} else {
			instances = getDelegate().get();
		}

		if(StringUtils.hasText(finalRequestZone)) {
			return instances.map(instanceList -> instanceList.stream().filter(
									instance -> this.multiMainZoneConfig.getMainZone().equals(instance.getMetadata().get(MAIN_ZONE))
											&& finalRequestZone.equals(instance.getMetadata().get(SUBSET_ZONE)))
							.collect(Collectors.toList())).filter(instanceList -> !instanceList.isEmpty())
					.switchIfEmpty(instances.map(instanceList -> instanceList.stream().filter(
									instance -> this.multiMainZoneConfig.getMainZone().equals(instance.getMetadata().get(MAIN_ZONE))
											&& this.multiMainZoneConfig.getMainZone().equals(instance.getMetadata().get(SUBSET_ZONE)))
							.collect(Collectors.toList())));
		}
		return instances.map(instanceList -> instanceList.stream().filter(
						instance -> this.multiMainZoneConfig.getMainZone().equals(instance.getMetadata().get(MAIN_ZONE))
								&& this.multiMainZoneConfig.getMainZone().equals(instance.getMetadata().get(SUBSET_ZONE)))
				.collect(Collectors.toList()));

	}
}
