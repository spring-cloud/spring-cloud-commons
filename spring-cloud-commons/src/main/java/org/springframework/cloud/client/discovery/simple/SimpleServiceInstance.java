package org.springframework.cloud.client.discovery.simple;

import java.net.URI;
import java.util.Map;

import org.springframework.cloud.client.ServiceInstance;

/**
 * Represents a Simple property based {@link ServiceInstance}
 *
 * @author Biju Kunjummen
 */
public class SimpleServiceInstance implements ServiceInstance {

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
