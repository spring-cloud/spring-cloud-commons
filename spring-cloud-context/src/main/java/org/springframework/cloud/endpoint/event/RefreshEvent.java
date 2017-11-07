package org.springframework.cloud.endpoint.event;

import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.ApplicationEvent;

/**
 * Event that triggers a call to {@link RefreshEndpoint#refresh()}
 * @author Spencer Gibb
 */
@SuppressWarnings("serial")
public class RefreshEvent extends ApplicationEvent {

	private Object event;
	private String eventDesc;

	public RefreshEvent(Object source, Object event, String eventDesc) {
		super(source);
		this.event = event;
		this.eventDesc = eventDesc;
	}

	public Object getEvent() {
		return this.event;
	}

	public String getEventDesc() {
		return this.eventDesc;
	}
}
