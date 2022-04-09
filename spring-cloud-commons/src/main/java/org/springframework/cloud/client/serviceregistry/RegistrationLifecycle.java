package org.springframework.cloud.client.serviceregistry;

import org.springframework.core.Ordered;

/**
 * Service registration life cycle. This life cycle is only related to Registration.
 *
 * @author huifer
 */
public interface RegistrationLifecycle<R extends Registration> extends Ordered {

	int DEFAULT_ORDER = 0;

	/**
	 * A method executed before registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessBeforeStartRegister(R registration);

	/**
	 * A method executed after registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessAfterStartRegister(R registration);

	/**
	 * A method executed before de-registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessBeforeStopRegister(R registration);

	/**
	 * A method executed after de-registering the local service with the
	 * {@link ServiceRegistry}.
	 * @param registration registration
	 */
	void postProcessAfterStopRegister(R registration);

	default int getOrder() {
		return DEFAULT_ORDER;
	}

}
