package org.springframework.cloud.client.loadbalancer;

import org.springframework.web.client.RestTemplate;

/**
 * @author Spencer Gibb
 */
public interface RestTemplateCustomizer {
	void customize(RestTemplate restTemplate);
}
