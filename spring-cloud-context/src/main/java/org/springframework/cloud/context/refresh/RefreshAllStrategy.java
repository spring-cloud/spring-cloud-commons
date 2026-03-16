package org.springframework.cloud.context.refresh;

import java.util.Set;

public class RefreshAllStrategy implements RefreshStrategy {
    @Override
    public Set<String> refresh(ContextRefresher contextRefresher, Set<String> propertiesToRefresh) {
        return contextRefresher.refreshEnvironment();
    }
}