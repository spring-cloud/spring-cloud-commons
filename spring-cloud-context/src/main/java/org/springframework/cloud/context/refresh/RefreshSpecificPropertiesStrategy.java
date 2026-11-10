package org.springframework.cloud.context.refresh;

import java.util.Set;

public class RefreshSpecificPropertiesStrategy implements RefreshStrategy {
    @Override
    public Set<String> refresh(ContextRefresher contextRefresher, Set<String> propertiesToRefresh) {
        return contextRefresher.refreshSpecificEnvironment(propertiesToRefresh);
    }
}