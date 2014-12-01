package org.springframework.cloud.client.discovery;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author Spencer Gibb
 */
@Slf4j
public class DiscoveryHealthIndicator extends AbstractHealthIndicator implements ApplicationContextAware {

    private ApplicationContext context;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            DiscoveryClient client = context.getBean(DiscoveryClient.class);
            if (client == null) {
                builder.unknown().withDetail("warning", "No DiscoveryClient found");
                return;
            }
            List<ServiceInstance> instances = client.getAllInstances();
            builder.up().withDetail("instances", instances);
        } catch (Exception e) {
            log.error("Error", e);
            builder.down(e);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.context = context;
    }
}
