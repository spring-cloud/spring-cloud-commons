package org.springframework.cloud.client.serviceregistry;

/**
 * Contract to register and deregister instances with a Service Registry.
 *
 * @author Spencer Gibb
 * @since 1.2.0
 */
public interface ServiceRegistry<R extends Registration> {

	/**
	 * Registers the registration. A registration typically has information about
	 * an instance, such as its hostname and port.
	 * @param registration The registration.
	 */
	void register(R registration);

	/**
	 * Deregisters the registration.
	 * @param registration
	 */
	void deregister(R registration);

	/**
	 * Closes the ServiceRegistry. This is a lifecycle method.
	 */
	void close();

	/**
	 * Sets the status of the registration. The status values are determined
	 * by the individual implementations.
	 *
	 * @see org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint
	 * @param registration The registration to update.
	 * @param status The status to set.
	 */
	void setStatus(R registration, String status);

	/**
	 * Gets the status of a particular registration.
	 *
	 * @see org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint
	 * @param registration The registration to query.
	 * @param <T> The type of the status.
	 * @return The status of the registration.
	 */
	<T> T getStatus(R registration);
}
