package org.springframework.cloud.client.circuitbreaker;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * @author Ryan Baxter
 */
public interface CircuitBreaker<T> {

	/**
	 * User can pass in a function which returns something.
	 * In Hystrix, this would return a HystrixCommand
	 * In R4J it is the actual code that we want to wrap in the circuit breaker.
	 * @return
	 */
	public T run(Function f);
}
