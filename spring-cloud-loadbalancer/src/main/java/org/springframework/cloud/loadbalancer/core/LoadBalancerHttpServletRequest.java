/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.cloud.loadbalancer.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.Charset;
import java.security.Principal;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import org.jspecify.annotations.Nullable;

import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * An {@link HttpServletRequest} implementation that is built from a {@link RequestData}
 * object. This is used to allow for passing a {@code HttpServletRequest} to components
 * that require it, but for which we only have {@link RequestData} to construct it.
 *
 * <p>
 * Note: This is a lightweight implementation. Various operation are not supported and
 * will throw an {@link UnsupportedOperationException}.
 *
 * @author Olga Maciaszek-Sharma
 * @since 5.0.0
 * @see BlockingApiVersionServiceInstanceListSupplier
 */
public class LoadBalancerHttpServletRequest implements HttpServletRequest {

	private final @Nullable RequestData requestData;

	public LoadBalancerHttpServletRequest(@Nullable RequestData requestData) {
		this.requestData = requestData;
	}

	@Override
	public @Nullable String getAuthType() {
		if (requestData == null || requestData.getHeaders() == null) {
			return null;
		}
		String authHeader = requestData.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authHeader == null) {
			return null;
		}
		if (authHeader.startsWith("Basic ")) {
			return HttpServletRequest.BASIC_AUTH;
		}
		if (authHeader.startsWith("Digest ")) {
			return HttpServletRequest.DIGEST_AUTH;
		}
		if (authHeader.startsWith("Bearer ")) {
			return "BEARER";
		}
		return null;
	}

	@Override
	public Cookie[] getCookies() {
		if (requestData == null) {
			return new Cookie[0];
		}
		MultiValueMap<String, String> cookies = requestData.getCookies();
		if (cookies == null || cookies.isEmpty()) {
			return new Cookie[0];
		}
		return cookies.entrySet()
			.stream()
			.flatMap(entry -> entry.getValue()
				.stream()
				.map(cookieEntryValue -> new Cookie(entry.getKey(), cookieEntryValue)))
			.toArray(Cookie[]::new);
	}

	@Override
	public long getDateHeader(String name) {
		String headerValue = (requestData == null || requestData.getHeaders() == null) ? null
				: requestData.getHeaders().getFirst(name);
		if (headerValue == null) {
			return -1L;
		}

		try {
			ZonedDateTime dateTime = ZonedDateTime.parse(headerValue, DateTimeFormatter.RFC_1123_DATE_TIME);
			return dateTime.toInstant().toEpochMilli();
		}
		catch (DateTimeParseException exception) {
			throw new IllegalArgumentException(
					"Cannot convert header [" + name + "] value [" + headerValue + "] to Date", exception);
		}
	}

	@Override
	public @Nullable String getHeader(String name) {
		if (requestData == null || requestData.getHeaders() == null) {
			return null;
		}
		return requestData.getHeaders().getFirst(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		if (requestData == null || requestData.getHeaders() == null) {
			return Collections.emptyEnumeration();
		}
		List<String> headerValues = requestData.getHeaders().get(name);
		return headerValues != null ? Collections.enumeration(headerValues) : Collections.emptyEnumeration();
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		if (requestData == null || requestData.getHeaders() == null) {
			return Collections.emptyEnumeration();
		}
		HttpHeaders headers = requestData.getHeaders();
		Set<String> headerNames = headers.headerNames();
		return Collections.enumeration(headerNames);
	}

	@Override
	public int getIntHeader(String name) {
		if (requestData == null || requestData.getHeaders() == null) {
			return -1;
		}
		String headerValue = requestData.getHeaders().getFirst(name);
		if (headerValue == null) {
			return -1;
		}
		try {
			return Integer.parseInt(headerValue);
		}
		catch (NumberFormatException e) {
			throw new NumberFormatException("Cannot convert header [" + name + "] value [" + headerValue + "] to int");
		}
	}

	@Override
	public @Nullable String getMethod() {
		if (requestData == null) {
			return null;
		}
		return requestData.getHttpMethod().name();
	}

	@Override
	public @Nullable String getPathInfo() {
		if (requestData == null) {
			return null;
		}
		URI uri = requestData.getUrl();
		return uri.getPath();
	}

	@Override
	public String getPathTranslated() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public String getContextPath() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public @Nullable String getQueryString() {
		if (requestData == null) {
			return null;
		}
		return requestData.getUrl().getRawQuery();
	}

	@Override
	public @Nullable String getRemoteUser() {
		Principal principal = getUserPrincipal();
		return principal != null ? principal.getName() : null;
	}

	@Override
	public boolean isUserInRole(String role) {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public @Nullable Principal getUserPrincipal() {
		if (requestData == null) {
			return null;
		}
		Object principal = requestData.getAttributes().get(Principal.class.getName());
		if (principal instanceof Principal) {
			return (Principal) principal;
		}
		return null;
	}

	@Override
	public String getRequestedSessionId() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public @Nullable String getRequestURI() {
		if (requestData == null) {
			return null;
		}
		return requestData.getUrl().getRawPath();
	}

	@Override
	public @Nullable StringBuffer getRequestURL() {
		if (requestData == null) {
			return null;
		}
		URI uri = requestData.getUrl();
		StringBuffer url = new StringBuffer();
		url.append(uri.getScheme()).append("://").append(uri.getHost());

		int port = uri.getPort();
		if (port != -1 && !(("http".equals(uri.getScheme()) && port == 80)
				|| ("https".equals(uri.getScheme()) && port == 443))) {
			url.append(':').append(port);
		}
		url.append(uri.getRawPath());
		return url;
	}

	@Override
	public String getServletPath() {
		return "";
	}

	@Override
	public HttpSession getSession(boolean create) {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public HttpSession getSession() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public void login(String username, String password) throws ServletException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public void logout() throws ServletException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		return Collections.emptyList();
	}

	@Override
	public @Nullable Part getPart(String name) throws IOException, ServletException {
		return null;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> httpUpgradeHandlerClass)
			throws IOException, ServletException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public @Nullable Object getAttribute(String name) {
		if (requestData == null) {
			return null;
		}
		return requestData.getAttributes().get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		if (requestData == null) {
			return Collections.emptyEnumeration();
		}
		return Collections.enumeration(requestData.getAttributes().keySet());
	}

	@Override
	public @Nullable String getCharacterEncoding() {
		if (requestData == null || requestData.getHeaders() == null) {
			return null;
		}
		String contentTypeHeader = requestData.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
		if (contentTypeHeader == null) {
			return null;
		}
		try {
			Charset charset = MediaType.parseMediaType(contentTypeHeader).getCharset();
			if (charset == null) {
				return null;
			}
			return charset.name();
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public void setCharacterEncoding(String encoding) throws UnsupportedEncodingException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public int getContentLength() {
		long contentLength = getContentLengthLong();
		if (contentLength > Integer.MAX_VALUE) {
			return -1;
		}
		return (int) contentLength;
	}

	@Override
	public long getContentLengthLong() {
		if (requestData == null || requestData.getHeaders() == null) {
			return -1L;
		}
		String contentLength = requestData.getHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
		if (contentLength == null) {
			return -1L;
		}
		try {
			return Long.parseLong(contentLength);
		}
		catch (NumberFormatException e) {
			return -1L;
		}
	}

	@Override
	public @Nullable String getContentType() {
		if (requestData == null || requestData.getHeaders() == null) {
			return null;
		}
		return requestData.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public @Nullable String getParameter(String name) {
		String[] values = getParameterMap().get(name);
		return values != null && values.length > 0 ? values[0] : null;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(getParameterMap().keySet());
	}

	@Override
	@SuppressWarnings("NullAway")
	public String[] getParameterValues(String name) {
		return getParameterMap().get(name);
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (requestData == null) {
			return Collections.emptyMap();
		}
		return UriComponentsBuilder.fromUri(requestData.getUrl())
			.build()
			.getQueryParams()
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().toArray(new String[0])));
	}

	@Override
	public String getProtocol() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public @Nullable String getScheme() {
		if (requestData == null) {
			return null;
		}
		return requestData.getUrl().getScheme();
	}

	@Override
	public @Nullable String getServerName() {
		if (requestData == null) {
			return null;
		}
		return requestData.getUrl().getHost();
	}

	@Override
	public int getServerPort() {
		int port = -1;
		if (requestData != null) {
			port = requestData.getUrl().getPort();
		}
		if (port == -1) {
			return "https".equals(getScheme()) ? 443 : 80;
		}
		return port;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public String getRemoteAddr() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public String getRemoteHost() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public void setAttribute(String name, @Nullable Object o) {
		if (o == null) {
			removeAttribute(name);
		}
		else if (requestData != null) {
			requestData.getAttributes().put(name, o);
		}
	}

	@Override
	public void removeAttribute(String name) {
		if (requestData != null) {
			requestData.getAttributes().remove(name);
		}
	}

	@Override
	public Locale getLocale() {
		return Locale.getDefault();
	}

	@Override
	public Enumeration<Locale> getLocales() {
		return Collections.enumeration(Collections.singletonList(Locale.getDefault()));
	}

	@Override
	public boolean isSecure() {
		if (requestData == null) {
			return false;
		}
		return "https".equalsIgnoreCase(requestData.getUrl().getScheme());
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public int getRemotePort() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public @Nullable String getLocalName() {
		return getServerName();
	}

	@Override
	public String getLocalAddr() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public int getLocalPort() {
		return getServerPort();
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
			throws IllegalStateException {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public boolean isAsyncStarted() {
		return false;
	}

	@Override
	public boolean isAsyncSupported() {
		return false;
	}

	@Override
	public @Nullable AsyncContext getAsyncContext() {
		return null;
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public String getRequestId() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public String getProtocolRequestId() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

	@Override
	public ServletConnection getServletConnection() {
		throw new UnsupportedOperationException("Not available for " + getClass().getSimpleName());
	}

}
