package org.springframework.cloud.client.circuitbreaker;

/**
 * @author Ryan Baxter
 */
public interface CircuitBreakerExecutor {

	public void run(CircuitBreaker cb);
}
