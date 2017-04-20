package org.springframework.cloud.client.serviceregistry;

import org.springframework.cloud.client.discovery.AbstractDiscoveryLifecycle;

/**
 * Lifecycle methods that may be useful and common to {@link ServiceRegistry} implementations.
 *
 * TODO: document the lifecycle
 *
 * @param <R> registration type passed to the {@link ServiceRegistry}.
 *
 * @author Spencer Gibb
 */
@SuppressWarnings("deprecation")
public abstract class AbstractAutoServiceRegistration<R extends Registration> extends AbstractDiscoveryLifecycle implements AutoServiceRegistration {

	private ServiceRegistry<R> serviceRegistry;

	protected AbstractAutoServiceRegistration(ServiceRegistry<R> serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	protected ServiceRegistry<R> getServiceRegistry() {
		return this.serviceRegistry;
	}

	protected abstract R getRegistration();

	protected abstract R getManagementRegistration();

	/**
	 * Register the local service with the {@link ServiceRegistry}
	 */
	@Override
	protected void register() {
		this.serviceRegistry.register(getRegistration());
	}

	/**
	 * Register the local management service with the {@link ServiceRegistry}
	 */
	@Override
	protected void registerManagement() {
		R registration = getManagementRegistration();
		if (registration != null) {
			this.serviceRegistry.register(registration);
		}
	}

	/**
	 * De-register the local service with the {@link ServiceRegistry}
	 */
	@Override
	protected void deregister() {
		this.serviceRegistry.deregister(getRegistration());
	}

	/**
	 * De-register the local management service with the {@link ServiceRegistry}
	 */
	@Override
	protected void deregisterManagement() {
		R registration = getManagementRegistration();
		if (registration != null) {
			this.serviceRegistry.deregister(registration);
		}
	}

	@Override
	public void stop() {
		if (this.getRunning().compareAndSet(true, false) && isEnabled()) {
			deregister();
			if (shouldRegisterManagement()) {
				deregisterManagement();
			}
			this.serviceRegistry.close();
		}
	}

}
