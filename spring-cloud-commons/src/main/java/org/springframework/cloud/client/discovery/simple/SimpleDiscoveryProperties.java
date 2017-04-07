package org.springframework.cloud.client.discovery.simple;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Properties to hold the details of a
 * {@link org.springframework.cloud.client.discovery.DiscoveryClient} service instances
 * for a given service
 *
 * @author Biju Kunjummen
 */

@ConfigurationProperties(prefix = "spring.cloud.discovery.client.simple")
public class SimpleDiscoveryProperties {
	private Map<String, List<SimpleServiceInstance>> instances = new HashMap<>();

	public Map<String, List<SimpleServiceInstance>> getInstances() {
		return instances;
	}

	public void setInstances(Map<String, List<SimpleServiceInstance>> instances) {
		this.instances = instances;
	}

	public static class SimpleServiceInstance implements ServiceInstance {

		private URI resolvedUri;
		private String host;
		private int port;
		private boolean secure;

		public SimpleServiceInstance() {
		}

		public SimpleServiceInstance(String uri) {
			setUri(uri);
		}

		public void setUri(String uri) {
			this.resolvedUri = URI.create(uri);
			this.host = this.resolvedUri.getHost();
			this.port = this.resolvedUri.getPort();
			String scheme = this.resolvedUri.getScheme();
			if ("https".equals(scheme)) {
				this.secure = true;
			}
		}

		@Override
		public String getServiceId() {
			return null;
		}

		@Override
		public String getHost() {
			return this.host;
		}

		@Override
		public int getPort() {
			return this.port;
		}

		@Override
		public boolean isSecure() {
			return this.secure;
		}

		@Override
		public URI getUri() {
			return this.resolvedUri;
		}

		@Override
		public Map<String, String> getMetadata() {
			return null;
		}
	}
}
