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

package org.springframework.cloud.client.serviceregistry;

import java.util.Collection;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * {@link RegistrationLifecycle} interface is implemented by default.
 *
 * @param <R> Registration
 * @author huifer
 */
public class DefaultRegistrationLifecycle<R extends Registration>
		implements RegistrationLifecycle<R>, ApplicationContextAware {

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void postProcessBeforeStartRegister(R registration) {
		Map<String, ServiceInstanceMetadataHandler> beansOfType = applicationContext
				.getBeansOfType(ServiceInstanceMetadataHandler.class);
		Collection<ServiceInstanceMetadataHandler> values = beansOfType.values();
		for (ServiceInstanceMetadataHandler value : values) {
			value.settingMetaData(registration);
		}
	}

	@Override
	public void postProcessAfterStartRegister(R registration) {

	}

	@Override
	public void postProcessBeforeStopRegister(R registration) {

	}

	@Override
	public void postProcessAfterStopRegister(R registration) {

	}

	@Override
	public int getOrder() {
		return RegistrationLifecycle.super.getOrder();
	}

}
