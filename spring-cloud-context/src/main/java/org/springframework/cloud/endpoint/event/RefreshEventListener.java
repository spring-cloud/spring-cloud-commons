package org.springframework.cloud.endpoint.event;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.endpoint.RefreshEndpoint;
import org.springframework.context.event.EventListener;

import lombok.extern.apachecommons.CommonsLog;

/**
 * Calls {@link RefreshEndpoint#refresh()} when a {@link RefreshEvent} is received.
 * Only responds to {@link RefreshEvent} after receiving an {@link ApplicationReadyEvent} as the RefreshEvent's might come to early in the application lifecycle.
 * @author Spencer Gibb
 */
@CommonsLog
public class RefreshEventListener {
	private RefreshEndpoint refresh;
	private AtomicBoolean ready = new AtomicBoolean(false);

	public RefreshEventListener(RefreshEndpoint refresh) {
		this.refresh = refresh;
	}

	@EventListener
	public void handle(ApplicationReadyEvent event) {
		this.ready.compareAndSet(false, true);
	}

	@EventListener
	public void handle(RefreshEvent event) {
		if (this.ready.get()) { // don't handle events before app is ready
			log.debug("Event received " + event.getEventDesc());
			String[] keys = this.refresh.refresh();
			log.info("Refresh keys changed: " + Arrays.asList(keys));
		}
	}
}
