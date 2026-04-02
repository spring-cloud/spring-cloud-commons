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

import org.jspecify.annotations.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.grpc.client.GrpcClientFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author KouShenhai（laokou）
 */
public class GrpcClientBeanPostProcessor implements BeanPostProcessor {

	private final GrpcClientFactory grpcClientFactory;

	public GrpcClientBeanPostProcessor(GrpcClientFactory grpcClientFactory) {
		this.grpcClientFactory = grpcClientFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object bean, @NonNull String beanName)
			throws BeansException {
		Class<?> clazz = bean.getClass();
		do {
			setFields(bean, clazz);
			setMethods(bean, clazz);
			clazz = clazz.getSuperclass();
		}
		while (!ObjectUtils.isEmpty(clazz));
		return bean;
	}

	private void setFields(Object bean, Class<?> clazz) {
		for (Field field : clazz.getDeclaredFields()) {
			GrpcClient grpcClient = AnnotationUtils.findAnnotation(field, GrpcClient.class);
			if (!ObjectUtils.isEmpty(grpcClient)) {
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, bean, getClient(field.getType(), grpcClient));
			}
		}
	}

	private void setMethods(Object bean, Class<?> clazz) {
		for (Method method : clazz.getDeclaredMethods()) {
			GrpcClient grpcClient = AnnotationUtils.findAnnotation(method, GrpcClient.class);
			if (!ObjectUtils.isEmpty(grpcClient)) {
				ReflectionUtils.makeAccessible(method);
				ReflectionUtils.invokeMethod(method, bean, getClient(method.getParameterTypes()[0], grpcClient));
			}
		}
	}

	private <T> T getClient(Class<T> type, @NonNull GrpcClient grpcClient) {
		String target = String.format("discovery://%s", getClientName(grpcClient));
		return grpcClientFactory.getClient(target, type, null);
	}

	private String getClientName(GrpcClient grpcClient) {
		String value = grpcClient.value();
		if (!StringUtils.hasText(value)) {
			value = grpcClient.name();
		}
		if (StringUtils.hasText(value)) {
			return value;
		}
		throw new IllegalStateException("Either 'name' or 'value' must be provided in @GrpcClient");
	}

}
