package org.springframework.cloud.context.refresh;

import java.util.Set;

public interface RefreshStrategy {
    Set<String> refresh(ContextRefresher contextRefresher, Set<String> propertiesToRefresh);
}