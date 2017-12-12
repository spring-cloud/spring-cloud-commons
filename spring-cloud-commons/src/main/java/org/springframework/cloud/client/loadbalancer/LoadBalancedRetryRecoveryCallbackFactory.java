package org.springframework.cloud.client.loadbalancer;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RecoveryCallback;

/**
 * Factory class to return the implementation of the RecoveryCallback.
 * @author Gang Li
 */
public interface LoadBalancedRetryRecoveryCallbackFactory {

    /**
     * Create a RecoveryCallback implementation class.
     * @param service On behalf of serviceId.
     * @return A {@link RecoveryCallback} implementation.
     */
    RecoveryCallback<ClientHttpResponse> createRecoveryCallback(String service);

    class NoRecoveryCallbackFactory implements LoadBalancedRetryRecoveryCallbackFactory {

        @Override
        public RecoveryCallback<ClientHttpResponse> createRecoveryCallback(String service) {
            return null;
        }
    }
}
