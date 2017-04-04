package org.springframework.cloud.client.serviceregistry;

/**
 * Contract to register and deregister instances with a Service Registry.
 *
 * @author Spencer Gibb
 * @since 1.2.0
 */
public interface ServiceRegistry<R extends Registration> {

	/**
	 * Register the registration. Registrations typically have information about
	 * instances such as: hostname and port.
	 * @param registration the registraion
	 */
	void register(R registration);

	/**
	 * Deregister the registration.
	 * @param registration
	 */
	void deregister(R registration);

	/**
	 * Close the ServiceRegistry. This a lifecycle method.
	 */
	void close();

	/**
	 * Sets the status of the registration. The status values are determined
	 * by the individual implementations.
	 *
	 * @see org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint
	 * @param registration the registration to update
	 * @param status the status to set
	 */
	void setStatus(R registration, String status);

	/**
	 * Gets the status of a particular registration.
	 *
	 * @see org.springframework.cloud.client.serviceregistry.endpoint.ServiceRegistryEndpoint
	 * @param registration the registration to query
	 * @param <T> the type of the status
	 * @return the status of the registration
	 */
	<T> T getStatus(R registration);
}
