package org.springframework.cloud.loadbalancer.blocking;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestTransformer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;


/**
 * author Gandhimathi Velusamy
 * to add X-forwarded Host and X-Forwarded Proto
 */
public class LoadBalancerXforwardTransformer implements LoadBalancerRequestTransformer {

	private final LoadBalancerProperties.Xforwarded xforwardedHeaders;

	public LoadBalancerXforwardTransformer(){
		xforwardedHeaders=null;
	}

	@Override
	public HttpRequest transformRequest(HttpRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}
		assert xforwardedHeaders != null;
		if(xforwardedHeaders.isAddxforwarded()==false)
			return request;

		HttpHeaders headers = request.getHeaders();

		String xforwardedHost = "";
		xforwardedHost += request.getURI().getHost();
		String xproto = request.getURI().getScheme();
		//headers.put(HttpHeaders.X-Forwarded)
		headers.add("X-Forwarded-host", xforwardedHost);
		headers.add("X-Forwarded-proto", xproto);
		return request;
	}
}
