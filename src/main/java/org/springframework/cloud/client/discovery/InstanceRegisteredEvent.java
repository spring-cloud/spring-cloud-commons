package org.springframework.cloud.client.discovery;

import org.springframework.context.ApplicationEvent;

/**
 * @author Spencer Gibb
 */
public class InstanceRegisteredEvent<T> extends ApplicationEvent {
    private T config;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the component that published the event (never {@code null})
     * @param config the configuration of the instance
     */
    public InstanceRegisteredEvent(Object source, T config) {
        super(source);
        this.config = config;
    }

    public T getConfig() {
        return config;
    }
}
