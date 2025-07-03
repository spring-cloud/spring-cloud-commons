package org.springframework.cloud.client.circuitbreaker.httpservice;

/**
 * @author Olga Maciaszek-Sharma
 */
class Fallbacks {

	String test(String description, int value) {
		return description + ": " + value;
	}
}
