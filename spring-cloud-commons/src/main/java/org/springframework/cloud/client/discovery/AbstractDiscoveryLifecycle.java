/*
 * Copyright 2013-2015 the original author or authors.
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

package org.springframework.cloud.client.discovery;

import javax.annotation.PreDestroy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;

/**
 * Lifecycle methods that may be useful and common to various DiscoveryClient implementations.
 * @author Spencer Gibb
 */
public abstract class AbstractDiscoveryLifecycle implements DiscoveryLifecycle,
		ApplicationContextAware {

	private boolean autoStartup = true;

	private boolean running;

	private int order = 0;

	private ApplicationContext context;

	private Environment environment;

	protected ApplicationContext getContext() {
		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.context = applicationContext;
		this.environment = this.context.getEnvironment();
	}

	protected Environment getEnvironment() {
		return environment;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		if (!isEnabled()) {
			return;
		}

		register();
		if (shouldRegisterManagement()) {
			registerManagement();
		}
		this.context
				.publishEvent(new InstanceRegisteredEvent<>(this, getConfiguration()));
		this.running = true;
	}

	/**
	 * @return if the management service should be registered with the DiscoveryService
	 */
	protected boolean shouldRegisterManagement() {
		return getManagementServerProperties() != null
				&& getManagementPort() != null
				&& ManagementServerPortUtils.isDifferent(this.context);
	}

	/**
	 * @return the object used to configure the DiscoveryClient
	 */
	protected abstract Object getConfiguration();

	/**
	 * Register the local service with the DiscoveryClient
	 */
	protected abstract void register();

	/**
	 * Register the local management service with the DiscoveryClient
	 */
	protected void registerManagement() {
	}

	/**
	 * De-register the local service with the DiscoveryClient
	 */
	protected abstract void deregister();

	/**
	 * De-register the local management service with the DiscoveryClient
	 */
	protected void deregisterManagement() {
	}

	/**
	 * @return if the DiscoveryClient is enabled
	 */
	protected abstract boolean isEnabled();

	/**
	 * @return the serviceId of the Management Service
	 */
	protected String getManagementServiceId() {
		return this.context.getId() + ":management";
		// TODO: configurable management suffix
	}

	/**
	 * @return the service name of the Management Service
	 */
	protected String getManagementServiceName() {
		return getAppName() + ":management";
		// TODO: configurable management suffix
	}

	/**
	 * @return the management server port
	 */
	protected Integer getManagementPort() {
		return getManagementServerProperties().getPort();
	}

	private ManagementServerProperties getManagementServerProperties() {
		try {
			return this.context.getBean(ManagementServerProperties.class);
		} catch (NoSuchBeanDefinitionException e) {
			return null;
		}
	}

	/**
	 * @return the app name, currently the spring.application.name property
	 */
	protected String getAppName() {
		return this.environment.getProperty("spring.application.name");
	}

	@Override
	public void stop() {
		if (isEnabled()) {
			deregister();
			if (shouldRegisterManagement()) {
				deregisterManagement();
			}
		}
		this.running = false;
	}

	@PreDestroy
	public void destroy() {
		stop();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public int getPhase() {
		return 0;
	}

}
