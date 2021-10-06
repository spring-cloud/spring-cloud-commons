package org.springframework.cloud.client.loadbalancer.reactive;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;

public class LoadBalanceXforwardTransformer implements LoadBalancerClientRequestTransformer {

	private LoadBalancerProperties.Xforwarded xforwardedHeader;

	@Override
	public ClientRequest transformRequest(ClientRequest request, ServiceInstance instance) {
		if (instance == null) {
			return request;
		}

		if (xforwardedHeader.isAddxforwarded()) {
			HttpHeaders headers = request.headers();

			String xforwardedHost = "";
			xforwardedHost += request.url().getHost();
			String xproto = request.url().getScheme();
			// headers.put(HttpHeaders.X-Forwarded)
			headers.add("X-Forwarded-host", xforwardedHost);
			headers.add("X-Forwarded-proto", xproto);
		}
		return request;
	}

}
