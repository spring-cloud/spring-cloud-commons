package org.springframework.cloud.client.circuitbreaker;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 *
 * @author Ryan Baxter
 */
public interface CircuitBreaker {

	public <T> T run(Supplier<T> toRun);

	public <T> T run(Supplier<T> toRun, Function<Throwable, T> fallback);

}
