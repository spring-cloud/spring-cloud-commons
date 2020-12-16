package org.springframework.cloud.client.loadbalancer;

/**
 * @author Olga Maciaszek-Sharma
 */
public interface TimedRequestContext {

	long getRequestStartTime();

	void setRequestStartTime(long requestStartTime);

}
