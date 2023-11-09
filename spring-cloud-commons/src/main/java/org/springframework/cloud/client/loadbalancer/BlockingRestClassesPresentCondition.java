/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.cloud.client.loadbalancer;

import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Verifies whether either {@link RestTemplate} or {@link RestClient} class is present.
 *
 * @author Olga Maciaszek-Sharma
 * @since 4.1.0
 */
public final class BlockingRestClassesPresentCondition extends AnyNestedCondition {

	private BlockingRestClassesPresentCondition() {
		super(ConfigurationPhase.REGISTER_BEAN);
	}

	@ConditionalOnClass(RestTemplate.class)
	static class RestTemplatePresent {

	}

	@ConditionalOnClass(RestClient.class)
	static class RestClientPresent {

	}

}
