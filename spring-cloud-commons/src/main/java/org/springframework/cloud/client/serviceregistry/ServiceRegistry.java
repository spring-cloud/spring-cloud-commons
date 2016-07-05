package org.springframework.cloud.client.serviceregistry;

/**
 * TODO: write javadoc
 * @author Spencer Gibb
 */
public interface ServiceRegistry<R> {
	void register(R registration);

	void deregister(R registration);

	void close();

}
