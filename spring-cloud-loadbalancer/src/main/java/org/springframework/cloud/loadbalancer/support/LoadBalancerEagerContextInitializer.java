/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.cloud.loadbalancer.support;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

/**
 * @author Andrii Bohutskyi
 */
public class LoadBalancerEagerContextInitializer implements ApplicationListener<ApplicationReadyEvent> {

	private final LoadBalancerClientFactory factory;

	private final List<String> serviceNames;

	public LoadBalancerEagerContextInitializer(LoadBalancerClientFactory factory, List<String> serviceNames) {
		this.factory = factory;
		this.serviceNames = serviceNames;
	}

	@Override
	public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
		serviceNames.forEach(factory::getInstance);
	}

}
