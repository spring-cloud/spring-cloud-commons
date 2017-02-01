package org.springframework.cloud.client.serviceregistry;

/**
 * @author Spencer Gibb
 */
public interface Registration {

	/**
	 * @return the serviceId associated with this registration
	 */
	String getServiceId();
}
