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

package org.springframework.cloud.endpoint;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.cloud.context.refresh.ContextRefresher;

/**
 * @author Dave Syer
 * @author Venil Noronha
 */
@Endpoint(id = "refresh")
public class RefreshEndpoint {

	private static final Log LOG = LogFactory.getLog(RefreshEndpoint.class);

	private final ContextRefresher contextRefresher;

	public RefreshEndpoint(ContextRefresher contextRefresher) {
		this.contextRefresher = contextRefresher;
	}

	@WriteOperation
	public Collection<String> refresh() {
		Set<String> keys = this.contextRefresher.refresh();
		LOG.info("Refreshed keys : " + keys);
		return keys;
	}

}
