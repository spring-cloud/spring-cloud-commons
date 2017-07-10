package org.springframework.cloud.client.loadbalancer.impl.context;

/**
 * @author Spencer Gibb
 */
public interface LoadBalancer<T> {
	enum Status {
		SUCCESSS,  // Request was handled successfully
		FAILED, // Request reached the server but failed due to timeout or internal error
		DISCARD, // Request did not go off box and should not be counted for statistics
	}

	// Context created for each request.
	interface Context<T> {
		boolean hasServer();

		T getServer();

		// Notification that the request completed
		void complete(Status status);
	}

	// Choose the next server based on the load balancing algorithm
	Context<T> choose();
}
