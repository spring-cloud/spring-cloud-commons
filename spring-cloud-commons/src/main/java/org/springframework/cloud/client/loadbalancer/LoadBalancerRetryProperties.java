/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.client.loadbalancer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the {@link LoadBalancerClient}.
 * @author Ryan Baxter
 */
@ConfigurationProperties("spring.cloud.loadbalancer.retry")
public class LoadBalancerRetryProperties {
    private boolean enabled = true;

    /**
     * Returns true if the load balancer should retry failed requests.
     * @return true if the load balancer should retry failed request, false otherwise.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets whether the load balancer should retry failed request.
     * @param enabled whether the load balancer should retry failed requests
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
