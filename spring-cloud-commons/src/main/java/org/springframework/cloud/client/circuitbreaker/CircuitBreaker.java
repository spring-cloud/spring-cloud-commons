package org.springframework.cloud.client.circuitbreaker;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface for common circuit breaker implementations.
 * @author Ryan Baxter
 */
public interface CircuitBreaker<T> {

	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback);

}
