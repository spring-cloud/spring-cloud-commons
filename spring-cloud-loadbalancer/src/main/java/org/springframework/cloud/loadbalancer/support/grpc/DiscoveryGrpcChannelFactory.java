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

package org.springframework.cloud.loadbalancer.support.grpc;

import io.grpc.ChannelCredentials;
import io.grpc.netty.NettyChannelBuilder;
import org.jspecify.annotations.NonNull;
import org.springframework.grpc.client.ClientInterceptorsConfigurer;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.NettyGrpcChannelFactory;

import java.util.List;

/**
 * Discovery Grpc ChannelFactory.
 *
 * @author KouShenhai（laokou）
 */
public class DiscoveryGrpcChannelFactory extends NettyGrpcChannelFactory {

	/**
	 * 构建通道工厂实例.
	 * @param globalCustomizers 要应用于所有已创建频道的全局自定义设置
	 * @param interceptorsConfigurer 配置已创建通道上的客户端拦截器
	 */
	public DiscoveryGrpcChannelFactory(
			List<GrpcChannelBuilderCustomizer<@NonNull NettyChannelBuilder>> globalCustomizers,
			ClientInterceptorsConfigurer interceptorsConfigurer) {
		super(globalCustomizers, interceptorsConfigurer);
		setVirtualTargets((p) -> p.substring(12));
	}

	@Override
	public boolean supports(String target) {
		return target.startsWith("discovery:");
	}

	@NonNull
	@Override
	protected NettyChannelBuilder newChannelBuilder(@NonNull String target, @NonNull ChannelCredentials credentials) {
		return NettyChannelBuilder.forTarget(String.format("discovery://%s", target), credentials);
	}

}
