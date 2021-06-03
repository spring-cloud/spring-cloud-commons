/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.cloud.commons.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2AutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ClientConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * Adds an MVC interceptor for relaying OAuth2 access tokens into the client context (if
 * there is one). In this way an incoming request to a resource server can be relayed
 * downstream just be using <code>@EnableOAuth2Client</code> and an
 * <code>OAuth2RestTemplate</code>. An MVC interceptor is used so as to have a minimal
 * impact on the call stack. If you are not using MVC you could use a custom filter or AOP
 * interceptor wrapping the same call to an {@link AccessTokenContextRelay}.
 *
 * <br/>
 *
 * N.B. an app that is using {@link UserInfoTokenServices} generally doesn't need this
 * interceptor, but it doesn't hurt to include it.
 *
 * @author Dave Syer
 *
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(OAuth2AutoConfiguration.class)
@ResourceServerTokenRelayAutoConfiguration.ConditionalOnOAuth2ClientInResourceServer
@ConditionalOnClass(ResourceServerConfiguration.class)
@ConditionalOnWebApplication
@ConditionalOnProperty(value = "spring.cloud.mvc.token-relay.enabled", matchIfMissing = true)
public class ResourceServerTokenRelayAutoConfiguration {

	@Bean
	public AccessTokenContextRelay accessTokenContextRelay(OAuth2ClientContext context) {
		return new AccessTokenContextRelay(context);
	}

	/**
	 * A {@link WebMvcConfigurer} for the access token interceptor.
	 *
	 * @author Dave Syer
	 *
	 */
	@Configuration(proxyBeanMethods = false)
	public static class ResourceServerTokenRelayRegistrationAutoConfiguration implements WebMvcConfigurer {

		@Autowired
		AccessTokenContextRelay accessTokenContextRelay;

		@Override
		public void addInterceptors(InterceptorRegistry registry) {
			registry.addInterceptor(

					new HandlerInterceptorAdapter() {
						@Override
						public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
								Object handler) throws Exception {
							accessTokenContextRelay.copyToken();
							return true;
						}
					}

			);
		}

	}

	@Target({ ElementType.TYPE, ElementType.METHOD })
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Conditional(OAuth2OnClientInResourceServerCondition.class)
	@interface ConditionalOnOAuth2ClientInResourceServer {

	}

	private static class OAuth2OnClientInResourceServerCondition extends AllNestedConditions {

		OAuth2OnClientInResourceServerCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnBean(ResourceServerConfiguration.class)
		static class Server {

		}

		@ConditionalOnBean(OAuth2ClientConfiguration.class)
		static class Client {

		}

	}

}
