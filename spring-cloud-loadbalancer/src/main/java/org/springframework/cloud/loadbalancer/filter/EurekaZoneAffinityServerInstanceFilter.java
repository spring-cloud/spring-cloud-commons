/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.filter;

import java.util.Map;

import io.micrometer.core.instrument.util.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;

/**
 * @author Hash.Jang
 */
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
