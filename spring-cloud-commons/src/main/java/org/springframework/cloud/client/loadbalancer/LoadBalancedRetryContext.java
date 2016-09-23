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

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.http.HttpRequest;
import org.springframework.retry.RetryContext;
import org.springframework.retry.context.RetryContextSupport;

/**
 * {@link RetryContext} for load balanced retries.
 * @author Ryan Baxter
 */
public class LoadBalancedRetryContext extends RetryContextSupport {

    private HttpRequest request;
    private ServiceInstance serviceInstance;

    /**
     * Creates a new load balanced context.
     * @param parent the parent context
     * @param request the request that is being load balanced
     */
    public LoadBalancedRetryContext(RetryContext parent, HttpRequest request) {
        super(parent);
        this.request = request;
    }

    /**
     * Gets the request that is being load balanced.
     * @return the request that is being load balanced
     */
    public HttpRequest getRequest() {
        return request;
    }

    /**
     * Sets the request that is being load baalnced.
     * @param request the request to load balanced
     */
    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    /**
     * Gets the service instance used during the retry.
     * @return the service instance used during the retry
     */
    public ServiceInstance getServiceInstance() {
        return serviceInstance;
    }

    /**
     * Sets the service instance to use during the retry.
     * @param serviceInstance the service instance to use during the retry
     */
    public void setServiceInstance(ServiceInstance serviceInstance) {
        this.serviceInstance = serviceInstance;
    }
}
