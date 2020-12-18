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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;

/**
 * Convenience class for relaying an access token from the {@link SecurityContext} to the
 * {@link OAuth2ClientContext}. If successful then subsequent calls to an
 * {@link OAuth2RestTemplate} using the context contained here will use the same access
 * token. This is mostly useful for relaying calls to a resource server downstream to
 * other resource servers. If the access token expires there is no way to refresh it, so
 * expect an exception from downstream (propagating it to the caller is the best strategy,
 * so they can refresh it and try again).
 *
 * @author Dave Syer
 *
 */
public class AccessTokenContextRelay {

	private OAuth2ClientContext context;

	public AccessTokenContextRelay(OAuth2ClientContext context) {
		this.context = context;
	}

	/**
	 * Attempt to copy an access token from the security context into the oauth2 context.
	 * @return true if the token was copied
	 */
	public boolean copyToken() {
		if (context.getAccessToken() == null) {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null) {
				Object details = authentication.getDetails();
				if (details instanceof OAuth2AuthenticationDetails) {
					OAuth2AuthenticationDetails holder = (OAuth2AuthenticationDetails) details;
					String token = holder.getTokenValue();
					DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken(token);
					String tokenType = holder.getTokenType();
					if (tokenType != null) {
						accessToken.setTokenType(tokenType);
					}
					context.setAccessToken(accessToken);
					return true;
				}
			}
		}
		return false;
	}

}
