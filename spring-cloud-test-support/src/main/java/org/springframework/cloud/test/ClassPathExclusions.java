/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.cloud.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Taken from Spring Boot test utils. https://github.com/spring-projects/spring-boot/blob/
 * 1.4.x/spring-boot/src/test/java/org/springframework/boot/testutil/ClassPathExclusions.java
 * @author Ryan Baxter
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClassPathExclusions {

	/**
	 * One or more Ant-style patterns that identify entries to be excluded from the class
	 * path. Matching is performed against an entry's {@link java.io.File#getName() file
	 * name}. For example, to exclude Hibernate Validator from the classpath,
	 * {@code "hibernate-validator-*.jar"} can be used.
	 * @return the exclusion patterns
	 */
	String[] value();

}
