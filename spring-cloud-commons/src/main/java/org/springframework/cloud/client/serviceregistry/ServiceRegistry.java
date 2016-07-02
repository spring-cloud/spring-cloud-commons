package org.springframework.cloud.client.serviceregistry;

/**
 * @author Spencer Gibb
 */
public interface ServiceRegistry<R> {
	void register(R registration);

	void deregister(R registration);
}
