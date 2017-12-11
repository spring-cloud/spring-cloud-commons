package org.springframework.cloud.client.loadbalancer;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RecoveryCallback;

/**
 * @author Gang Li
 */
public interface LoadBalancedRetryRecoveryCallbackFactory {

    RecoveryCallback<ClientHttpResponse> createRecoveryCallback(String service);

    class NoRecoveryCallbackFactory implements LoadBalancedRetryRecoveryCallbackFactory {

        @Override
        public RecoveryCallback<ClientHttpResponse> createRecoveryCallback(String service) {
            return null;
        }
    }
}
