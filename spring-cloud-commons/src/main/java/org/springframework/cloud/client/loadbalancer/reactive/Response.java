package org.springframework.cloud.client.loadbalancer.reactive;

/**
 * Response created for each request.
  */
public interface Response<T> {
	boolean hasServer();

	T getServer();

	/**
	 * Notification that the request completed
	 * @param completionContext
	 */
	void onComplete(CompletionContext completionContext);
}
