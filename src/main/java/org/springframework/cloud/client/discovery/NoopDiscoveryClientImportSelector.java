/*
 * Copyright 2013-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client.discovery;

import lombok.extern.apachecommons.CommonsLog;

import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Import Selector that only returns a NoopDiscoveryClientConfiguration if the
 * @EnableDiscoveryClient annotation has NOT been used.
 * @author Spencer Gibb
 */
@CommonsLog
public class NoopDiscoveryClientImportSelector implements
		DeferredImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(EnableDiscoveryClient.class.getName(), true));

		if (attributes == null) {
			// @EnableDiscoveryClient has not been used, so configure the noop client
			return new String[] { NoopDiscoveryClientConfiguration.class.getName() };
		}

		return new String[]{};
	}
}
