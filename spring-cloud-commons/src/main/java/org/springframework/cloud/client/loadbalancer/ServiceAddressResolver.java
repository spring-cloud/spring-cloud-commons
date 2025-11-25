package org.springframework.cloud.client.loadbalancer;

import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;

import java.net.URI;

/**
 * ServiceAddressResolver is responsible for resolving service addresses to ServiceInstance objects.
 * 
 * <p>This class provides functionality to determine if a URI represents a direct address call
 * and to convert such addresses into ServiceInstance representations that can be used by
 * load balancer implementations.</p>
 *
 * @author xzxiaoshan
 * @since 2025-11-20 13:24:08
 */
public class ServiceAddressResolver {

	/**
	 * Determines whether the given URI represents a direct address call.
	 * 
	 * <p>A URI is considered a direct address if:
	 * <ul>
	 *   <li>A port is explicitly specified (not microservice service name calls, 
	 *       as ports are obtained from instances in the registry)</li>
	 *   <li>The host contains a colon, which may indicate IPv6</li>
	 *   <li>The host contains a dot, which indicates an IPv4 address or domain name</li>
	 *   <li>The host is "localhost"</li>
	 * </ul>
	 * </p>
	 * 
	 * @param uri the URI to check
	 * @return true if the URI represents a direct address call, false otherwise
	 */
	public boolean isDirectAddress(URI uri) {
		String host = uri.getHost();
		int port = uri.getPort();
		// 1. Port specified definitely not a microservice service name call, 
		//    as ports are obtained from instances in the registry
		// 2. Host containing colons may be IPv6, containing dots are IPv4 addresses or domain names
		return port >= 0 || host.contains(".") || host.contains(":") || host.equalsIgnoreCase("localhost");
	}

	/**
	 * Determines whether the given request represents a direct address call.
	 * 
	 * <p>This method extracts the URI from the request and delegates to 
	 * {@link #isDirectAddress(URI)} for the actual check.</p>
	 * 
	 * @param request the load balancer request to check
	 * @return true if the request represents a direct address call, false otherwise
	 * @throws IllegalArgumentException if unable to extract URI from request context
	 */
	public boolean isDirectAddress(Request<?> request) {
		URI uri = this.getUriFromRequest(request);
		return this.isDirectAddress(uri);
	}

	/**
	 * Gets the service ID from the given URI.
	 * 
	 * <p>If the URI represents a direct address with an explicitly specified port,
	 * the service ID will be constructed as "host:port". Otherwise, just the host
	 * will be returned as the service ID.</p>
	 * 
	 * @param uri the URI to extract service ID from
	 * @return the service ID derived from the URI
	 */
	public String getServiceId(URI uri) {
		String host = uri.getHost();
		int port = uri.getPort();
		if (this.isDirectAddress(uri) && port >= 0) {
			return host + ":" + port;
		} else {
			return host;
		}
	}

	/**
	 * Extracts the URI from the given request.
	 * 
	 * <p>This protected method handles different context types that might contain 
	 * URI information and extracts the URI accordingly.</p>
	 * 
	 * @param request the load balancer request containing context information
	 * @return the URI extracted from the request
	 * @throws IllegalArgumentException if unable to extract URI from request context
	 */
	protected URI getUriFromRequest(Request<?> request) {
		// Extract the URI from the request context
		Object context = request.getContext();
		URI uri = null;

		// Handle different context types that might contain URI information
		if (context instanceof DefaultRequestContext) {
			Object clientRequest = ((DefaultRequestContext) context).getClientRequest();
			if (clientRequest instanceof RequestData) {
				uri = ((RequestData) clientRequest).getUrl();
			}
		} else if (context instanceof RequestData) {
			uri = ((RequestData) context).getUrl();
		}

		// If we couldn't extract URI from context, return null or throw exception
		if (uri == null) {
			throw new IllegalArgumentException("Unable to extract URI from request context");
		}

		return uri;
	}

	/**
	 * Converts a direct address to a ServiceInstance.
	 * 
	 * <p>This method extracts the URI from the request context and creates a 
	 * ServiceInstance representation that can be used by load balancer implementations.</p>
	 * 
	 * @param request the load balancer request containing context information
	 * @return a ServiceInstance representation of the direct address
	 * @throws IllegalArgumentException if unable to extract URI from request context
	 */
	public ServiceInstance createDirectServiceInstance(Request<?> request) {
		URI uri = this.getUriFromRequest(request);

		String serviceId = this.getServiceId(uri);
		String host = uri.getHost();
		int port = uri.getPort();
		boolean secure = "https".equalsIgnoreCase(uri.getScheme()) ||
				"wss".equalsIgnoreCase(uri.getScheme());

		return new DefaultServiceInstance(null, serviceId, host, port, secure);
	}
}