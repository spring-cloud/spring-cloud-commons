/*
 * Copyright 2013-2016 the original author or authors.
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
package org.springframework.cloud.client.loadbalancer;

/**
 * Retry logic to use for the {@link LoadBalancerClient}.
 * @author Ryan Baxter
 */
public interface LoadBalancedRetryPolicy {

    /**
     * Return true to retry the failed request on the same server.
     * This method may be called more than once when executing a single operation.
     * @param context the context for the retry operation
     * @return true to retry the failed request on the same server, false otherwise
     */
    public boolean canRetrySameServer(LoadBalancedRetryContext context);

    /**
     * Return true to retry the failed request on the next server from the load balancer.
     * This method may be called more than once when executing a single operation.
     * @param context the context for the retry operation
     * @return true to retry the failed request on the next server from the load balancer, false otherwise
     */
    public boolean canRetryNextServer(LoadBalancedRetryContext context);

    /**
     * Called when the retry operation has ended.
     * @param context the context for the retry operation
     */
    public abstract void close(LoadBalancedRetryContext context);

    /**
     * Called when the execution fails.
     * @param context the context for the retry operation
     * @param throwable the throwable from the failed execution.
     */
    public abstract void registerThrowable(LoadBalancedRetryContext context, Throwable throwable);
}
