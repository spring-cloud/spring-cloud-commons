package org.springframework.cloud.endpoint.event;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.event.EventListener;

/**
 * Calls {@link RefreshEventListener#refresh} when a {@link RefreshEvent} is received.
 * Only responds to {@link RefreshEvent} after receiving an {@link ApplicationReadyEvent} as the RefreshEvent's might come to early in the application lifecycle.
 * @author Spencer Gibb
 */
public class RefreshEventListener {
	private static Log log = LogFactory.getLog(RefreshEventListener.class);
	private ContextRefresher refresh;
	private AtomicBoolean ready = new AtomicBoolean(false);

	public RefreshEventListener(ContextRefresher refresh) {
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
			Set<String> keys = this.refresh.refresh();
			log.info("Refresh keys changed: " + keys);
		}
	}
}
