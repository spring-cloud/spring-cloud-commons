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

package org.springframework.cloud.loadbalancer.config;

/**
 * @author seal
 */
public class LoadBalancerMultiMainZoneConfig {

	/**
	 * main zone
	 * recommend DAILY PRE PROD
	 * default DAILY
	 */
	private String mainZone;

	private String zoneRequestHeaderKey;

	public LoadBalancerMultiMainZoneConfig(String mainZone, String zoneRequestHeaderKey) {
		this.mainZone = mainZone;
		this.zoneRequestHeaderKey = zoneRequestHeaderKey;
	}

	public String getMainZone() {
		return mainZone;
	}

	public void setMainZone(String mainZone) {
		this.mainZone = mainZone;
	}

	public String getZoneRequestHeaderKey() {
		return zoneRequestHeaderKey;
	}

	public void setZoneRequestHeaderKey(String zoneRequestHeaderKey) {
		this.zoneRequestHeaderKey = zoneRequestHeaderKey;
	}
}
