package org.springframework.cloud.client.serviceregistry;

/**
 * @author Spencer Gibb
 */
public interface ServiceRegistry<R extends Registration> {
	void register(R registration);

	void deregister(R registration);
}
