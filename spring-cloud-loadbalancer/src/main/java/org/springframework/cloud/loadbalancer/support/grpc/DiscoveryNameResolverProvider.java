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

package org.springframework.cloud.loadbalancer.support.grpc;

import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.event.EventListener;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Discovery NameResolver Provider.
 *
 * @author KouShenhai（laokou）
 */
public class DiscoveryNameResolverProvider extends NameResolverProvider {

	private final Logger logger = LoggerFactory.getLogger(DiscoveryNameResolverProvider.class);

	private final DiscoveryClient discoveryClient;

	private final ExecutorService executorService;

	private final Set<DiscoveryNameResolver> discoveryNameResolvers;

	public DiscoveryNameResolverProvider(DiscoveryClient discoveryClient, ExecutorService executorService) {
		this.discoveryClient = discoveryClient;
		this.executorService = executorService;
		this.discoveryNameResolvers = new HashSet<>();
	}

	@Override
	protected boolean isAvailable() {
		return true;
	}

	@Override
	protected int priority() {
		return 6;
	}

	@Override
	public NameResolver newNameResolver(URI uri, NameResolver.Args args) {
		DiscoveryNameResolver discoveryNameResolver = new DiscoveryNameResolver(uri.getHost(), discoveryClient,
				executorService);
		discoveryNameResolvers.add(discoveryNameResolver);
		return discoveryNameResolver;
	}

	@Override
	public String getDefaultScheme() {
		return "discovery";
	}

	@EventListener(HeartbeatEvent.class)
	public void onHeartbeatEvent(HeartbeatEvent event) {
		logger.debug("Received HeartbeatEvent, refreshing DiscoveryNameResolvers, event: {}", event.getValue());
		for (DiscoveryNameResolver discoveryNameResolver : discoveryNameResolvers) {
			discoveryNameResolver.refreshFromExternal();
		}
	}

}
