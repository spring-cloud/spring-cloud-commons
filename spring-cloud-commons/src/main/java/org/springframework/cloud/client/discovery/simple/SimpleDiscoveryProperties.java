package org.springframework.cloud.client.discovery.simple;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.ServiceInstance;

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

	/**
	 * The properties of the local instance (if it exists). Users should set these
	 * properties explicitly if they are exporting data (e.g. metrics) that need to be
	 * identified by the service instance.
	 */
	private SimpleServiceInstance local = new SimpleServiceInstance();

	public Map<String, List<SimpleServiceInstance>> getInstances() {
		return this.instances;
	}

	public void setInstances(Map<String, List<SimpleServiceInstance>> instances) {
		this.instances = instances;
	}

	public SimpleServiceInstance getLocal() {
		return this.local;
	}

	@PostConstruct
	public void init() {
		for (String key : this.instances.keySet()) {
			for (SimpleServiceInstance instance : this.instances.get(key)) {
				instance.setServiceId(key);
			}
		}
	}

	public static class SimpleServiceInstance implements ServiceInstance {

		/**
		 * The URI of the service instance. Will be parsed to extract the scheme, hos and
		 * port.
		 */
		private URI uri;
		private String host;
		private int port;
		private boolean secure;
		/**
		 * Metadata for the service instance. Can be used by discovery clients to modify
		 * their behaviour per instance, e.g. when load balancing.
		 */
		private Map<String, String> metadata = new LinkedHashMap<>();
		/**
		 * The identifier or name for the service. Multiple instances might share the same
		 * service id.
		 */
		private String serviceId;

		public SimpleServiceInstance() {
		}

		public SimpleServiceInstance(URI uri) {
			setUri(uri);
		}

		public void setUri(URI uri) {
			this.uri = uri;
			this.host = this.uri.getHost();
			this.port = this.uri.getPort();
			String scheme = this.uri.getScheme();
			if ("https".equals(scheme)) {
				this.secure = true;
			}
		}

		@Override
		public String getServiceId() {
			return this.serviceId;
		}

		public void setServiceId(String id) {
			this.serviceId = id;
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
			return this.uri;
		}

		@Override
		public Map<String, String> getMetadata() {
			return this.metadata;
		}
	}
}
