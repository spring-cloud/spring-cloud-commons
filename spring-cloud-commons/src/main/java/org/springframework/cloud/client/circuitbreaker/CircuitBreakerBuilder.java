package org.springframework.cloud.client.circuitbreaker;

/**
 * Constructs circuit breakers.
 *
 * @author Ryan Baxter
 */
public interface CircuitBreakerBuilder {

	public CircuitBreakerBuilder id(String id);

	public CircuitBreaker build();
}
