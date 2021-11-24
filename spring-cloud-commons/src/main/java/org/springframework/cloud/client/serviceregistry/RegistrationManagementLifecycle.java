package org.springframework.cloud.client.serviceregistry;

/**
 * Service registration life cycle. This life cycle is only related to
 * ManagementRegistration.
 *
 * @author huifer
 */
public interface RegistrationManagementLifecycle extends RegistrationLifecycle {

	/**
	 * A method executed before registering the local management service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	void postProcessBeforeStartRegisterManagement(Registration registrationManagement);

	/**
	 * A method executed after registering the local management service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	void postProcessAfterStartRegisterManagement(Registration registrationManagement);

	/**
	 * A method executed before de-registering the management local service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	void postProcessBeforeStopRegisterManagement(Registration registrationManagement);

	/**
	 * A method executed after de-registering the management local service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	void postProcessAfterStopRegisterManagement(Registration registrationManagement);

}
