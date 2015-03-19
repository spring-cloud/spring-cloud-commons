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

package org.springframework.cloud.util;

import org.junit.Test;
import org.springframework.cloud.util.SingleImplementationImportSelector;

import static org.junit.Assert.assertEquals;

/**
 * @author Spencer Gibb
 */
public class SingleImplementationImportSelectorTests {

	@Test
	public void testFindAnnotation() {
		MyAnnotationImportSelector selector = new MyAnnotationImportSelector();
		assertEquals("annotationClass was wrong", MyAnnotation.class,
				selector.getAnnotationClass());
	}

	public static @interface MyAnnotation {
	}

	public static class MyAnnotationImportSelector extends
			SingleImplementationImportSelector<MyAnnotation> {

		@Override
		protected boolean isEnabled() {
			return true;
		}

	}
}
