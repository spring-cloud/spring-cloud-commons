package org.springframework.cloud.context.refresh;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

public class RefreshCompleteEvent extends ApplicationEvent {

    private Set<String> keys;

    public RefreshCompleteEvent(Object source) {
        super(source);
    }

    public RefreshCompleteEvent(Object source, Set<String> keys) {
        super(source);
        this.keys = keys;
    }
}
