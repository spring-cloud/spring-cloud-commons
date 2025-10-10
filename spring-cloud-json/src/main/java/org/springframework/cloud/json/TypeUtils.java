/*
 * Copyright 2019-present the original author or authors.
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

package org.springframework.cloud.json;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.messaging.Message;
import org.springframework.util.ObjectUtils;

/**
 * Set of utility operations to interrogate function definitions.
 *
 * @author Oleg Zhurakousky
 * @author Andrey Shlykov
 * @author Artem Bilan
 * @since 3.0
 */
public final class TypeUtils {

	private static Log logger = LogFactory.getLog(TypeUtils.class);

	private TypeUtils() {

	}

	/**
	 * Will return 'true' if the provided type is a {@link Collection} type. This also
	 * includes collections wrapped in {@link Message}. For example, If provided type is
	 * {@code Message<List<Foo>>} this operation will return 'true'.
	 * @param type type to interrogate
	 * @return 'true' if this type represents a {@link Collection}. Otherwise 'false'.
	 */
	public static boolean isTypeCollection(Type type) {
		Class rawClass = getRawType(type);
		if (rawClass == null) {
			return false;
		}
		if (Collection.class.isAssignableFrom(getRawType(type))) {
			return true;
		}
		type = getGenericType(type);
		type = type == null ? Object.class : type;
		Class<?> rawType = type instanceof ParameterizedType ? getRawType(type) : (Class<?>) type;
		return Collection.class.isAssignableFrom(rawType) || JsonNode.class.isAssignableFrom(rawType);
	}

	public static boolean isTypeMap(Type type) {
		if (Map.class.isAssignableFrom(getRawType(type))) {
			return true;
		}
		type = getGenericType(type);
		Class<?> rawType = type instanceof ParameterizedType ? getRawType(type) : (Class<?>) type;
		return Map.class.isAssignableFrom(rawType);
	}

	public static boolean isTypeArray(Type type) {
		return type instanceof GenericArrayType;
	}

	public static boolean isJsonNode(Type type) {
		return getRawType(type).isArray();
	}

	/**
	 * A convenience method identical to {@link #getImmediateGenericType(Type, int)} for
	 * cases when provided 'type' is {@link Publisher} or {@link Message}.
	 * @param type type to interrogate
	 * @return generic type if possible otherwise the same type as provided
	 */
	public static Type getGenericType(Type type) {
		if (isPublisher(type) || isMessage(type)) {
			type = getImmediateGenericType(type, 0);
		}

		if (type instanceof WildcardType) {
			type = Object.class;
		}
		return type;
	}

	/**
	 * Effectively converts {@link Type} which could be {@link ParameterizedType} to raw
	 * Class (no generics).
	 * @param type actual {@link Type} instance
	 * @return instance of {@link Class} as raw representation of the provided
	 * {@link Type}
	 */
	public static Class<?> getRawType(Type type) {
		if (type instanceof WildcardType) {
			Type[] upperbounds = ((WildcardType) type).getUpperBounds();
			/*
			 * Kotlin may have something like this <? extends Message> which is
			 * technically a whildcard yet it has upper/lower types. See GH-1260
			 */
			return ObjectUtils.isEmpty(upperbounds) ? Object.class : getRawType(upperbounds[0]);
		}
		return ResolvableType.forType(type).getRawClass();
	}

	public static String discoverBeanDefinitionNameByQualifier(ListableBeanFactory beanFactory, String qualifier) {
		String[] candidateBeans = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, Object.class);

		for (String beanName : candidateBeans) {
			if (BeanFactoryAnnotationUtils.isQualifierMatch(qualifier::equals, beanName, beanFactory)) {
				return beanName;
			}
		}
		return null;
	}

	public static Type getImmediateGenericType(Type type, int index) {
		if (type instanceof ParameterizedType) {
			return ((ParameterizedType) type).getActualTypeArguments()[index];
		}
		return null;
	}

	public static boolean isPublisher(Type type) {
		return isFlux(type) || isMono(type);
	}

	public static boolean isFlux(Type type) {
		return getRawType(type) == Flux.class;
	}

	public static boolean isCollectionOfMessage(Type type) {
		if (isMessage(type) && (isTypeCollection(type) || isTypeArray(type))) {
			if (isTypeCollection(type)) {
				return isMessage(getImmediateGenericType(type, 0));
			}
			else if (type instanceof GenericArrayType arrayType) {
				return true;
			}
		}
		return false;
	}

	public static boolean isMessage(Type type) {
		if (isPublisher(type)) {
			type = getImmediateGenericType(type, 0);
		}
		if (type instanceof GenericArrayType arrayType) {
			type = arrayType.getGenericComponentType();
		}

		Class<?> resolveRawClass = TypeUtils.getRawType(type);
		if (type instanceof ParameterizedType && !Message.class.isAssignableFrom(resolveRawClass)) {
			type = getImmediateGenericType(type, 0);
		}
		resolveRawClass = TypeUtils.getRawType(type);
		if (resolveRawClass == null) {
			return false;
		}
		return Message.class.isAssignableFrom(resolveRawClass);
	}

	public static boolean isMono(Type type) {
		return getRawType(type) == Mono.class;
	}

	private static boolean isOfType(Type type, Class<?> cls) {
		if (type instanceof Class) {
			return cls.isAssignableFrom((Class<?>) type);
		}
		else if (type instanceof ParameterizedType) {
			return isOfType(((ParameterizedType) type).getRawType(), cls);
		}
		return false;
	}

	private static String discoverDefinitionName(String functionDefinition,
			GenericApplicationContext applicationContext) {
		String[] aliases = applicationContext.getAliases(functionDefinition);
		for (String alias : aliases) {
			if (applicationContext.getBeanFactory().containsBeanDefinition(alias)) {
				return alias;
			}
		}
		return functionDefinition;
	}

}
