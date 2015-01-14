package org.springframework.cloud.client.discovery;

import org.springframework.context.ApplicationEvent;

/**
 * Event to be published after the local service instance registers itself with a
 * discovery service
 * @author Spencer Gibb
 */
@SuppressWarnings("serial")
public class InstanceRegisteredEvent<T> extends ApplicationEvent {
	private T config;

	/**
	 * @param source the component that published the event (never {@code null})
	 * @param config the configuration of the instance
	 */
	public InstanceRegisteredEvent(Object source, T config) {
		super(source);
		this.config = config;
	}

	public T getConfig() {
		return this.config;
	}
}
