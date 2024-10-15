/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.stats;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.ResponseData;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Utility class for building metrics tags for load-balanced calls.
 *
 * @author Olga Maciaszek-Sharma
 * @author Jaroslaw Dembek
 * @since 3.0.0
 */
final class LoadBalancerTags {

	static final String UNKNOWN = "UNKNOWN";

	static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

	private LoadBalancerTags() {
		throw new UnsupportedOperationException("Cannot instantiate utility class");
	}

	static Iterable<Tag> buildSuccessRequestTags(CompletionContext<Object, ServiceInstance, Object> completionContext,
			boolean useUriTemplateAttribute) {
		ServiceInstance serviceInstance = completionContext.getLoadBalancerResponse().getServer();
		Tags tags = Tags.of(buildServiceInstanceTags(serviceInstance));
		Object clientResponse = completionContext.getClientResponse();
		if (clientResponse instanceof ResponseData responseData) {
			RequestData requestData = responseData.getRequestData();
			if (requestData != null) {
				tags = tags.and(valueOrUnknown("method", requestData.getHttpMethod()),
						valueOrUnknown("uri", getPath(requestData, useUriTemplateAttribute)));
			}
			else {
				tags = tags.and(Tag.of("method", UNKNOWN), Tag.of("uri", UNKNOWN));
			}

			tags = tags.and(Tag.of("outcome", forStatus(statusValue(responseData))),
					valueOrUnknown("status", statusValue(responseData)));
		}
		else {
			tags = tags.and(Tag.of("method", UNKNOWN), Tag.of("uri", UNKNOWN), Tag.of("outcome", UNKNOWN),
					Tag.of("status", UNKNOWN));
		}
		return tags;
	}

	// In keeping with the way null HttpStatus is handled in Actuator
	private static int statusValue(ResponseData responseData) {
		return responseData.getHttpStatus() != null ? responseData.getHttpStatus().value() : 200;
	}

	private static String getPath(RequestData requestData, boolean useUriTemplateAttribute) {
		if (useUriTemplateAttribute && requestData.getAttributes() != null) {
			var uriTemplate = (String) requestData.getAttributes().get(URI_TEMPLATE_ATTRIBUTE);
			if (uriTemplate != null) {
				return uriTemplate;
			}
		}
		return requestData.getUrl() != null ? requestData.getUrl().getPath() : UNKNOWN;
	}

	static Iterable<Tag> buildDiscardedRequestTags(CompletionContext<Object, ServiceInstance, Object> completionContext,
			boolean useUriTemplateAttribute) {
		if (completionContext.getLoadBalancerRequest().getContext() instanceof RequestDataContext) {
			RequestData requestData = ((RequestDataContext) completionContext.getLoadBalancerRequest().getContext())
				.getClientRequest();
			if (requestData != null) {
				return Tags.of(valueOrUnknown("method", requestData.getHttpMethod()),
						valueOrUnknown("uri", getPath(requestData, useUriTemplateAttribute)),
						valueOrUnknown("serviceId", getHost(requestData)));
			}
		}
		return Tags.of(valueOrUnknown("method", UNKNOWN), valueOrUnknown("uri", UNKNOWN),
				valueOrUnknown("serviceId", UNKNOWN));

	}

	private static String getHost(RequestData requestData) {
		return requestData.getUrl() != null ? requestData.getUrl().getHost() : UNKNOWN;
	}

	static Iterable<Tag> buildFailedRequestTags(CompletionContext<Object, ServiceInstance, Object> completionContext,
			boolean useUriTemplateAttribute) {
		ServiceInstance serviceInstance = completionContext.getLoadBalancerResponse().getServer();
		Tags tags = Tags.of(buildServiceInstanceTags(serviceInstance)).and(exception(completionContext.getThrowable()));
		if (completionContext.getLoadBalancerRequest().getContext() instanceof RequestDataContext) {
			RequestData requestData = ((RequestDataContext) completionContext.getLoadBalancerRequest().getContext())
				.getClientRequest();
			if (requestData != null) {
				return tags.and(Tags.of(valueOrUnknown("method", requestData.getHttpMethod()),
						valueOrUnknown("uri", getPath(requestData, useUriTemplateAttribute))));
			}
		}
		return tags.and(Tags.of(valueOrUnknown("method", UNKNOWN), valueOrUnknown("uri", UNKNOWN)));
	}

	static Iterable<Tag> buildServiceInstanceTags(ServiceInstance serviceInstance) {
		return Tags.of(valueOrUnknown("serviceId", serviceInstance.getServiceId()),
				valueOrUnknown("serviceInstance.instanceId", serviceInstance.getInstanceId()),
				valueOrUnknown("serviceInstance.host", serviceInstance.getHost()),
				valueOrUnknown("serviceInstance.port", String.valueOf(serviceInstance.getPort())));
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

	private static Tag exception(Throwable exception) {
		if (exception != null) {
			String simpleName = exception.getClass().getSimpleName();
			return Tag.of("exception", StringUtils.hasText(simpleName) ? simpleName : exception.getClass().getName());
		}
		return Tag.of("exception", "None");
	}

	// Logic from Actuator's `Outcome` class. Copied in here to avoid adding Actuator
	// dependency.
	public static String forStatus(int status) {
		if (status >= 100 && status < 200) {
			return "INFORMATIONAL";
		}
		else if (status >= 200 && status < 300) {
			return "SUCCESS";
		}
		else if (status >= 300 && status < 400) {
			return "REDIRECTION";
		}
		else if (status >= 400 && status < 500) {
			return "CLIENT_ERROR";
		}
		else if (status >= 500 && status < 600) {
			return "SERVER_ERROR";
		}
		return UNKNOWN;
	}

}
