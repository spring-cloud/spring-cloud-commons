package org.springframework.cloud.client.serviceregistry;

/**
 * A marker interface used by a {@link ServiceRegistry}.
 *
 * @author Spencer Gibb
 * @since 1.2.0
 */
public interface Registration {

	/**
	 * @return the serviceId associated with this registration
	 */
	String getServiceId();
}
