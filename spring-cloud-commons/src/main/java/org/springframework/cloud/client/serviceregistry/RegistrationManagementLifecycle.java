package org.springframework.cloud.client.serviceregistry;

/**
 * Service registration life cycle. This life cycle is only related to
 * ManagementRegistration.
 *
 * @author huifer
 */
public interface RegistrationManagementLifecycle<R extends Registration> extends RegistrationLifecycle<R> {

	/**
	 * A method executed before registering the local management service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	void postProcessBeforeStartRegisterManagement(R registrationManagement);

	/**
	 * A method executed after registering the local management service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	void postProcessAfterStartRegisterManagement(R registrationManagement);

	/**
	 * A method executed before de-registering the management local service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	void postProcessBeforeStopRegisterManagement(R registrationManagement);

	/**
	 * A method executed after de-registering the management local service with the
	 * {@link ServiceRegistry}.
	 * @param registrationManagement registrationManagement
	 */
	void postProcessAfterStopRegisterManagement(R registrationManagement);

}
