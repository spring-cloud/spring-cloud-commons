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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.cloud.client.discovery.event.InstanceRegisteredEvent;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

/**
 * Lifecycle methods that may be useful and common to {@link ServiceRegistry} implementations.
 *
 * TODO: document the lifecycle
 *
 * @param <R> registration type passed to the {@link ServiceRegistry}.
 *
 * @author Spencer Gibb
 */
//TODO: rename to AbstractServiceRegistryLifecycle or AbstractAutoServiceRegistration?
public abstract class AbstractDiscoveryLifecycle<R> implements DiscoveryLifecycle,
		ApplicationContextAware, ApplicationListener<EmbeddedServletContainerInitializedEvent> {

	private static final Log logger = LogFactory.getLog(AbstractDiscoveryLifecycle.class);

	private boolean autoStartup = true;

	private AtomicBoolean running = new AtomicBoolean(false);

	private int order = 0;

	private ApplicationContext context;

	private Environment environment;

	private AtomicInteger port = new AtomicInteger(0);

	private ServiceRegistry<R> serviceRegistry;

	protected AbstractDiscoveryLifecycle(ServiceRegistry<R> serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

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

	protected AtomicInteger getPort() {
		return port;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void stop(Runnable callback) {
		try {
			stop();
		} catch (Exception e) {
			logger.error("A problem occurred attempting to stop discovery lifecycle", e);
		}
		callback.run();
	}

	@Override
	public void start() {
		if (!isEnabled()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Discovery Lifecycle disabled. Not starting");
			}
			return;
		}

		// only set the port if the nonSecurePort is 0 and this.port != 0
		if (this.port.get() != 0 && getConfiguredPort() == 0) {
			setConfiguredPort(this.port.get());
		}
		// only initialize if nonSecurePort is greater than 0 and it isn't already running
		// because of containerPortInitializer below
		if (!this.running.get() && getConfiguredPort() > 0) {
			register();
			if (shouldRegisterManagement()) {
				registerManagement();
			}
			this.context.publishEvent(new InstanceRegisteredEvent<>(this,
					getConfiguration()));
			this.running.compareAndSet(false, true);
		}
	}

	protected abstract int getConfiguredPort();
	protected abstract void setConfiguredPort(int port);

	/**
	 * @return if the management service should be registered with the {@link ServiceRegistry}
	 */
	protected boolean shouldRegisterManagement() {
		return getManagementPort() != null && ManagementServerPortUtils.isDifferent(this.context);
	}

	/**
	 * @return the object used to configure the registration
	 */
	protected abstract Object getConfiguration();

	protected abstract R getRegistration();

	protected abstract R getManagementRegistration();

	protected ServiceRegistry<R> getServiceRegistry() {
		return this.serviceRegistry;
	}

	/**
	 * Register the local service with the {@link ServiceRegistry}
	 */
	protected void register() {
		this.serviceRegistry.register(getRegistration());
	}

	/**
	 * Register the local management service with the {@link ServiceRegistry}
	 */
	protected void registerManagement() {
		this.serviceRegistry.register(getManagementRegistration());
	}

	/**
	 * De-register the local service with the {@link ServiceRegistry}
	 */
	protected void deregister() {
		this.serviceRegistry.deregister(getRegistration());
	}

	/**
	 * De-register the local management service with the {@link ServiceRegistry}
	 */
	protected void deregisterManagement() {
		this.serviceRegistry.deregister(getManagementRegistration());
	}

	/**
	 * @return true, if the {@link DiscoveryLifecycle} is enabled
	 */
	protected abstract boolean isEnabled();

	/**
	 * @return the serviceId of the Management Service
	 */
	protected String getManagementServiceId() {
		// TODO: configurable management suffix
		return this.context.getId() + ":management";
	}

	/**
	 * @return the service name of the Management Service
	 */
	protected String getManagementServiceName() {
		// TODO: configurable management suffix
		return getAppName() + ":management";
	}

	/**
	 * @return the management server port
	 */
	protected Integer getManagementPort() {
		return ManagementServerPortUtils.getPort(this.context);
	}

	/**
	 * @return the app name, currently the spring.application.name property
	 */
	protected String getAppName() {
		return this.environment.getProperty("spring.application.name", "application");
	}

	@Override
	public void stop() {
		if (this.running.compareAndSet(true, false) && isEnabled()) {
			deregister();
			if (shouldRegisterManagement()) {
				deregisterManagement();
			}
		}
	}

	@PreDestroy
	public void destroy() {
		stop();
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	@Override
	public int getPhase() {
		return 0;
	}

	@Override
	public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
		// TODO: take SSL into account
		// Don't register the management port as THE port
		if (!"management".equals(event.getApplicationContext().getNamespace())) {
			this.port.compareAndSet(0, event.getEmbeddedServletContainer().getPort());
			this.start();
		}
	}
}
