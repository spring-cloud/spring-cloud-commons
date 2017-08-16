package org.springframework.cloud.client.loadbalancer.impl;

/**
 * @author Spencer Gibb
 */
public interface LoadBalancer<T> {

	/**
	 * Response created for each request.
 	 */
	interface Response<T> {
		boolean hasServer();

		T getServer();

		/**
		 * Notification that the request completed
		 * @param onComplete
		 */
		void onComplete(OnComplete onComplete);
	}

	interface Request {
		//TODO: define contents
	}

	/**
	 * Choose the next server based on the load balancing algorithm
	 * @param request
	 * @return
	 */
	Response<T> choose(Request request);
}
