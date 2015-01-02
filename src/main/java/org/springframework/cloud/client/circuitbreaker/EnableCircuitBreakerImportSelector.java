package org.springframework.cloud.client.circuitbreaker;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.cloud.client.SingleImplementationImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * @author Spencer Gibb
 */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class EnableCircuitBreakerImportSelector extends SingleImplementationImportSelector<EnableCircuitBreaker> {

	@Override
	protected boolean isEnabled() {
		return new RelaxedPropertyResolver(getEnvironment()).getProperty("spring.cloud.circuit.breaker.enabled", Boolean.class, Boolean.TRUE);
	}
}
