package org.springframework.cloud.loadbalancer.filter;

import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;

import java.util.Map;

public class EurekaZoneAffinityServerInstanceFilter implements ServerInstanceFilter<String> {
    private static Log log = LogFactory.getLog(EurekaZoneAffinityServerInstanceFilter.class);
    private final String currentZone;

    public EurekaZoneAffinityServerInstanceFilter(Environment environment) {
        this.currentZone = environment.getProperty("eureka.instance.metadata-map.zone");
    }

    @Override
    public boolean filter(ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        if (metadata != null) {
            String zone = metadata.get("zone");
            boolean b = StringUtils.isBlank(currentZone) || currentZone.equalsIgnoreCase(zone);
            log.info(serviceInstance.getInstanceId() + ", zone: " + zone + ", filter result " + b + ", current zone: " + currentZone);
            return b;
        }
        return true;
    }
}
