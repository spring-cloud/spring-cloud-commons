/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.Resource;

/**
 * Common client TLS properties.
 */
public class TlsProperties {

	private static final String DEFAULT_STORE_TYPE = "PKCS12";

	private static final Map<String, String> EXTENSION_STORE_TYPES = extTypes();

	private boolean enabled;

	private Resource keyStore;

	private String keyStoreType;

	private String keyStorePassword = "";

	private String keyPassword = "";

	private Resource trustStore;

	private String trustStoreType;

	private String trustStorePassword = "";

	private static Map<String, String> extTypes() {
		Map<String, String> result = new HashMap<>();

		result.put("p12", "PKCS12");
		result.put("pfx", "PKCS12");
		result.put("jks", "JKS");

		return Collections.unmodifiableMap(result);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Resource getKeyStore() {
		return keyStore;
	}

	public void setKeyStore(Resource keyStore) {
		this.keyStore = keyStore;
	}

	public String getKeyStoreType() {
		if (keyStore != null && keyStoreType == null) {
			keyStoreType = storeTypeOf(keyStore);
		}
		return keyStoreType;
	}

	public void setKeyStoreType(String keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public char[] keyStorePassword() {
		return keyStorePassword.toCharArray();
	}

	public String getKeyPassword() {
		return keyPassword;
	}

	public void setKeyPassword(String keyPassword) {
		this.keyPassword = keyPassword;
	}

	public char[] keyPassword() {
		return keyPassword.toCharArray();
	}

	public Resource getTrustStore() {
		return trustStore;
	}

	public void setTrustStore(Resource trustStore) {
		this.trustStore = trustStore;
	}

	public String getTrustStoreType() {
		if (trustStore != null && trustStoreType == null) {
			trustStoreType = storeTypeOf(trustStore);
		}
		return trustStoreType;
	}

	public void setTrustStoreType(String trustStoreType) {
		this.trustStoreType = trustStoreType;
	}

	public String getTrustStorePassword() {
		return trustStorePassword;
	}

	public void setTrustStorePassword(String trustStorePassword) {
		this.trustStorePassword = trustStorePassword;
	}

	public char[] trustStorePassword() {
		return trustStorePassword.toCharArray();
	}

	@Deprecated
	public void postConstruct() {
	}

	private String storeTypeOf(Resource resource) {
		String extension = fileExtensionOf(resource);
		String type = EXTENSION_STORE_TYPES.get(extension);

		return (type == null) ? DEFAULT_STORE_TYPE : type;
	}

	private String fileExtensionOf(Resource resource) {
		String name = resource.getFilename();
		int index = name.lastIndexOf('.');

		return index < 0 ? "" : name.substring(index + 1).toLowerCase();
	}

}
