package org.springframework.cloud.client.serviceregistry;

/**
 * TODO: write javadoc
 * @author Spencer Gibb
 */
public interface ServiceRegistry<R extends Registration> {
	void register(R registration);

	void deregister(R registration);

	void close();

	// TODO: return value for success?
	void setStatus(R registration, String status);

	// TODO: concrete return value? Interface?
	Object getStatus(R registration);
}
