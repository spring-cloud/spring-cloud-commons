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

	//TODO: add metrics
	class CompletionContext {
		final Status status;

		public CompletionContext(Status status) {
			this.status = status;
		}

		public Status getStatus() {
			return status;
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("CompletionContext{");
			sb.append("status=").append(status);
			sb.append('}');
			return sb.toString();
		}
	}

	// ChosenContext created for each request.
	interface ChosenContext<T> {
		boolean hasServer();

		T getServer();

		// Notification that the request completed
		void complete(CompletionContext completionContext);
	}

	interface RequestContext {

	}

	// Choose the next server based on the load balancing algorithm
	ChosenContext<T> choose(RequestContext requestContext);
}
