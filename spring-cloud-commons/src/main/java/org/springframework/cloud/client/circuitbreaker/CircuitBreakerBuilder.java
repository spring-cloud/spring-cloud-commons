package org.springframework.cloud.client.circuitbreaker;

/**
 * Constructs circuit breakers.
 *
 * @author Ryan Baxter
 */
public interface CircuitBreakerBuilder<C> {

	public CircuitBreakerBuilder id(String id);

	public CircuitBreakerBuilder configFactory(C configFactory);

	public CircuitBreaker build();
}
