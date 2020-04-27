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

package org.springframework.cloud.loadbalancer.annotation;

import java.util.Arrays;
import java.util.Objects;

import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 */
public class LoadBalancerClientSpecification
		implements NamedContextFactory.Specification {

	private String name;

	private Class<?>[] configuration;

	public LoadBalancerClientSpecification() {
	}

	public LoadBalancerClientSpecification(String name, Class<?>[] configuration) {
		Assert.hasText(name, "name must not be empty");
		this.name = name;
		Assert.notNull(configuration, "configuration must not be null");
		this.configuration = configuration;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		Assert.hasText(name, "name must not be empty");
		this.name = name;
	}

	public Class<?>[] getConfiguration() {
		return this.configuration;
	}

	public void setConfiguration(Class<?>[] configuration) {
		Assert.notNull(configuration, "configuration must not be null");
		this.configuration = configuration;
	}

	@Override
	public String toString() {
		ToStringCreator to = new ToStringCreator(this);
		to.append("name", this.name);
		to.append("configuration", this.configuration);
		return to.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LoadBalancerClientSpecification that = (LoadBalancerClientSpecification) o;
		return Objects.equals(this.name, that.name)
				&& Arrays.equals(this.configuration, that.configuration);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name, this.configuration);
	}

}
