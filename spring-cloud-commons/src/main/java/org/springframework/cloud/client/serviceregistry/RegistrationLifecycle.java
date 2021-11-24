package org.springframework.cloud.client.serviceregistry;

import org.springframework.core.Ordered;

/**
 * Service registration life cycle. This life cycle is only related to Registration.
 *
 * @author huifer
 */
public interface RegistrationLifecycle extends Ordered {

	int DEFAULT_ORDER = 0;

	/**
	 * A method executed before registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessBeforeStartRegister(Registration registration);

	/**
	 * A method executed after registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessAfterStartRegister(Registration registration);

	/**
	 * A method executed before de-registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessBeforeStopRegister(Registration registration);

	/**
	 * A method executed after de-registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessAfterStopRegister(Registration registration);

}
