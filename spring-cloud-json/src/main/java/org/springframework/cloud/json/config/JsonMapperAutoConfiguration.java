/*
 * Copyright 2013-present the original author or authors.
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

package org.springframework.cloud.json.config;

import com.fasterxml.jackson.databind.Module;
import com.google.gson.Gson;
import org.springframework.cloud.json.GsonMapper;
import org.springframework.cloud.json.JacksonMapper;
import org.springframework.cloud.json.JsonMapper;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.datatype.joda.JodaModule;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.KotlinDetector;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
public class JsonMapperAutoConfiguration {

	/**
	 * Property which defines preferred JSON mapper. Default is jackson.
	 */
	public static final String JSON_MAPPER_PROPERTY = "spring.cloud.preferred-json-mapper";

	@Bean
	@ConditionalOnMissingBean(JsonMapper.class)
	public JsonMapper jsonMapper(ApplicationContext context) {
		String preferredMapper = context.getEnvironment().getProperty(JSON_MAPPER_PROPERTY);
		if (StringUtils.hasText(preferredMapper)) {
			if ("gson".equals(preferredMapper)) {
				return gson(context);
			}
			else if ("jackson".equals(preferredMapper)) {
				return jackson(context);
			}
//			else if ("jackson2".equals(preferredMapper)) {
//				return jackson2(context);
//			}
		}
		else {
			if (ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", null)) {
				return jackson(context);
			}
			else if (ClassUtils.isPresent("com.google.gson.Gson", null)) {
				return gson(context);
			}
//			else if (ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", null)) {
//				return jackson2(context);
//			}
		}
		throw new IllegalStateException(
				"Failed to configure JsonMapper. Neither 'jackson' (2 or 3) nor 'gson' dependnecies are present on the claspath");
	}

	private JsonMapper gson(ApplicationContext context) {
		Assert.state(ClassUtils.isPresent("com.google.gson.Gson", ClassUtils.getDefaultClassLoader()),
				"Can not bootstrap Gson mapper since Gson is not on the classpath");
		Gson gson;
		try {
			gson = context.getBean(Gson.class);
		}
		catch (Exception e) {
			gson = new Gson();
		}
		return new GsonMapper(gson);
	}

	@SuppressWarnings("unchecked")
	private JsonMapper jackson(ApplicationContext context) {
		Assert.state(ClassUtils.isPresent("tools.jackson.databind.ObjectMapper", ClassUtils.getDefaultClassLoader()),
				"Can not bootstrap Jackson mapper since Jackson is not on the classpath");
		ObjectMapper mapper = null;
		MapperBuilder builder = tools.jackson.databind.json.JsonMapper.builder();
		try {
			builder = context.getBean(ObjectMapper.class).rebuild();
		}
		catch (Exception e) {
			builder = tools.jackson.databind.json.JsonMapper.builder();
		}
		builder = builder.addModule(new JodaModule());

		if (KotlinDetector.isKotlinPresent()) {
			try {
				Class<? extends JacksonModule> kotlinModuleClass = (Class<? extends JacksonModule>) ClassUtils
					.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", ClassUtils.getDefaultClassLoader());
				JacksonModule kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
				builder = builder.addModule(kotlinModule);
			}
			catch (ClassNotFoundException ex) {
				// jackson-module-kotlin not available
			}
		}
		builder = builder.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		builder = builder.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper = builder.build();
		return new JacksonMapper(mapper);
	}

//	@SuppressWarnings("unchecked")
//	private JsonMapper jackson2(ApplicationContext context) {
//		Assert.state(ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", ClassUtils.getDefaultClassLoader()),
//				"Can not bootstrap Jackson mapper since Jackson is not on the classpath");
//		com.fasterxml.jackson.databind.ObjectMapper mapper;
//		try {
//			mapper = context.getBean(com.fasterxml.jackson.databind.ObjectMapper.class).copy();
//		}
//		catch (Exception e) {
//			mapper = new com.fasterxml.jackson.databind.ObjectMapper();
//			mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
//		}
//		mapper.registerModule(new com.fasterxml.jackson.datatype.joda.JodaModule());
//		if (KotlinDetector.isKotlinPresent()) {
//			try {
//				if (!mapper.getRegisteredModuleIds().contains("com.fasterxml.jackson.module.kotlin.KotlinModule")) {
//					Class<? extends com.fasterxml.jackson.databind.Module> kotlinModuleClass = (Class<? extends com.fasterxml.jackson.databind.Module>)
//							ClassUtils.forName("com.fasterxml.jackson.module.kotlin.KotlinModule", ClassUtils.getDefaultClassLoader());
//					com.fasterxml.jackson.databind.Module kotlinModule = BeanUtils.instantiateClass(kotlinModuleClass);
//					mapper.registerModule(kotlinModule);
//				}
//			}
//			catch (ClassNotFoundException ex) {
//				// jackson-module-kotlin not available
//			}
//		}
//		mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
////			mapper.configure(DeserializationFeature.FAIL_ON_TRAILING_TOKENS, true);
//		mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//
//		return new JacksonMapper2(mapper);
//	}

}
