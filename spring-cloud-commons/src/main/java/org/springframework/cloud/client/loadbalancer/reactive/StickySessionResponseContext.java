package org.springframework.cloud.client.loadbalancer.reactive;

/**
 * @author Olga Maciaszek-Sharma
 */
public class StickySessionResponseContext extends DefaultResponseContext {

	private String affinityKey;

	String getAffinityKey() {
		return affinityKey;
	}
}
