package org.springframework.cloud.client.circuitbreaker;

/**
 * Creates circuit breakers based on the underlying implementation.
 *
 * @author Ryan Baxter
 */
public interface CircuitBreakerFactory {

	public CircuitBreaker create(String id);
}
