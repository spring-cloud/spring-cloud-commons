package org.springframework.cloud.loadbalancer.stats;

import java.util.function.Function;
import java.util.regex.Pattern;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import org.springframework.boot.actuate.metrics.http.Outcome;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

/**
 * @author Olga Maciaszek-Sharma
 */
public class LoadBalancerTags {

	private static final String UNKNOWN = "UNKNOWN";
	private static final Pattern PATTERN_BEFORE_PATH = Pattern
			.compile("^https?://[^/]+/");

	private LoadBalancerTags() {
		throw new UnsupportedOperationException("Cannot instantiate utility class");
	}

	static Iterable<Tag> buildLoadBalancedRequestTags(CompletionContext<Object, ServiceInstance, Object> completionContext) {
		Tags tags = Tags.of(Tag.of("lbCompletionStatus", completionContext.status()
				.toString()), exception(completionContext.getThrowable()));
		if (completionContext.getLoadBalancerResponse().hasServer()) {
			ServiceInstance serviceInstance = completionContext.getLoadBalancerResponse()
					.getServer();
			tags = tags.and(buildServiceInstanceTags(serviceInstance));
		}
		Object clientResponse = completionContext.getClientResponse();
		if (clientResponse instanceof ResponseData) {
			ResponseData responseData = (ResponseData) clientResponse;
			RequestData requestData = responseData.getRequestData();
			if (requestData != null) {
				tags = tags.and(valueOrUnknown("method", requestData.getHttpMethod()),
						valueOrUnknown("uri", requestData.getUrl().getPath()));
			}
			tags = tags.and(Outcome.forStatus(responseData.getHttpStatus().value())
							.asTag(),
					status(responseData.getHttpStatus()));
		}
		return tags;
	}

	static Iterable<Tag> buildServiceInstanceTags(ServiceInstance serviceInstance) {
		return Tags.of(valueOrUnknown("serviceId", serviceInstance.getServiceId()),
				valueOrUnknown("serviceInstance.instanceId", serviceInstance
						.getInstanceId()),
				valueOrUnknown("serviceInstance.host", serviceInstance.getHost()),
				valueOrUnknown("serviceInstance.port", String
						.valueOf(serviceInstance.getPort())),
				valueOrUnknown("serviceInstance.uri", serviceInstance
						.getUri(), extractPath()),
				valueOrUnknown("serviceInstance.secure", String
						.valueOf(serviceInstance.isSecure())));
	}

	private static Tag valueOrUnknown(String key, String value) {
		if (value != null) {
			return Tag.of(key, value);
		}
		return Tag.of(key, UNKNOWN);
	}

	private static Tag valueOrUnknown(String key, Object value) {
		if (value != null) {
			return Tag.of(key, String.valueOf(value));
		}
		return Tag.of(key, UNKNOWN);
	}

	private static Tag valueOrUnknown(String key, Object value, Function<String, String> supplier) {
		if (value != null) {
			return Tag.of(key, supplier.apply(value.toString()));
		}
		return Tag.of(key, UNKNOWN);
	}


	private static Function<String, String> extractPath() {
		return url -> {
			String path = PATTERN_BEFORE_PATH.matcher(url).replaceFirst("");
			return (path.startsWith("/") ? path : "/" + path);
		};
	}


	private static Tag status(HttpStatus status) {
		if (status == null) {
			status = HttpStatus.OK;
		}
		return Tag.of("status", String.valueOf(status.value()));
	}

	private static Tag exception(Throwable exception) {
		if (exception != null) {
			String simpleName = exception.getClass().getSimpleName();
			return Tag.of("exception", StringUtils
					.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}
		return Tag.of("exception", "None");
	}
}
