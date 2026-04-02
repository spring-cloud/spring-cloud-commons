/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.cloud.loadbalancer.annotation.grpc;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.grpc.client.GrpcClientFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author KouShenhai（laokou）
 */
class GrpcClientBeanPostProcessorTest {

	private GrpcClientBeanPostProcessor postProcessor;

	private final String mockClient = "mockClient";

	@BeforeEach
	void setUp() {
		GrpcClientFactory grpcClientFactory = mock(GrpcClientFactory.class);
		postProcessor = new GrpcClientBeanPostProcessor(grpcClientFactory);
		when(grpcClientFactory.getClient(ArgumentMatchers.eq("discovery://test-service"),
				ArgumentMatchers.eq(String.class), ArgumentMatchers.any()))
			.thenReturn(mockClient);
	}

	@Test
	void testFieldInjection() {
		FieldBean bean = new FieldBean();
		postProcessor.postProcessBeforeInitialization(bean, "fieldBean");
		Assertions.assertThat(bean.getClient()).isEqualTo(mockClient);
	}

	@Test
	void testMethodInjection() {
		MethodBean bean = new MethodBean();
		postProcessor.postProcessBeforeInitialization(bean, "methodBean");
		Assertions.assertThat(bean.getClient()).isEqualTo(mockClient);
	}

	@Test
	void testSuperclassInjection() {
		SubBean bean = new SubBean();
		postProcessor.postProcessBeforeInitialization(bean, "subBean");
		Assertions.assertThat(bean.getClient()).isEqualTo(mockClient);
	}

	static class FieldBean {

		@GrpcClient(value = "test-service")
		private String client;

		public void setClient(String client) {
			this.client = client;
		}

		public String getClient() {
			return client;
		}

	}

	static class MethodBean {

		private String client;

		@GrpcClient(value = "test-service")
		public void setClient(String client) {
			this.client = client;
		}

		public String getClient() {
			return client;
		}

	}

	static class BaseBean {

		@GrpcClient(value = "test-service")
		protected String client;

		public void setClient(String client) {
			this.client = client;
		}

		public String getClient() {
			return client;
		}

	}

	static class SubBean extends BaseBean {

	}

}
