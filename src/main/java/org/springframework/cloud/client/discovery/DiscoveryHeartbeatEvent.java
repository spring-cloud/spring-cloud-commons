package org.springframework.cloud.client.discovery;

import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
 */
@SuppressWarnings("serial")
public class DiscoveryHeartbeatEvent extends ApplicationEvent {

	private final Object value;

	public DiscoveryHeartbeatEvent(Object source, Object value) {
		super(source);
		this.value = value;
	}

	public Object getValue() {
		return this.value;
	}
}
