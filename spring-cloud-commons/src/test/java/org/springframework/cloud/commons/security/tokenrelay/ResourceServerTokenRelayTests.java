/*
 * Copyright 2015-2015 the original author or authors.
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

package org.springframework.cloud.commons.security.tokenrelay;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.commons.security.AccessTokenContextRelay;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * @author Peter Szanto (spring@szantocsalad.hu)
 *
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "security.oauth2.resource.jwt.keyValue=secret", "spring.cloud.mvc.token-relay.enabled=true",
				"spring.autoconfigure.exclude=" })
public class ResourceServerTokenRelayTests {

	protected static final String TOKEN_VALID_UNTIL_2085 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
			+ "eyJleHAiOjM2NDA2ODU4ODIsInVzZXJfbmFtZSI6InJlYWRlciIsImF1dGhvcml0aWVzIjpbIlJPTEVfUkVBREVSIl0s"
			+ "Imp0aSI6ImRkOTAzZGM2LTI0NDctNDViMi04MDZjLTIzZjU3ODVhNGQ4MCIsImNsaWVudF9pZCI6IndlYi1hcHAiLCJzY29wZSI6WyJyZWFkIl19."
			+ "6hoNtxmN1_o5Ki0D0ae4amSOTRmit3pmaqv-z1-Qk4Y";

	protected static final String AUTH_HEADER_TO_BE_RELAYED = "Bearer " + TOKEN_VALID_UNTIL_2085;

	protected static final String TEST_RESPONSE = "[\"test response\"]";

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private MockRestServiceServer mockServerToReceiveRelay;

	@SpyBean
	AccessTokenContextRelay accessTokenContextRelay;

	@Test
	public void tokenRelayJWT() throws Exception {

		mockServerToReceiveRelay.expect(requestTo("https://example.com/test"))
				.andExpect(header("authorization", AUTH_HEADER_TO_BE_RELAYED))
				.andRespond(withSuccess(TEST_RESPONSE, MediaType.APPLICATION_JSON));

		HttpEntity<String> authorizationHeader = createAuthorizationHeader();
		ResponseEntity<String> exchange = testRestTemplate.exchange("/token-relay", HttpMethod.GET, authorizationHeader,
				String.class);

		assertThat(exchange.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
		assertThat(exchange.getBody()).isEqualTo(TEST_RESPONSE);

		mockServerToReceiveRelay.verify();
		verify(accessTokenContextRelay).copyToken();
	}

	private HttpEntity<String> createAuthorizationHeader() {
		HttpHeaders headers = new HttpHeaders();
		headers.add("Authorization", AUTH_HEADER_TO_BE_RELAYED);
		return new HttpEntity<String>("parameters", headers);

	}

	@SpringBootApplication
	@TestConfiguration
	@EnableResourceServer
	@ComponentScan(basePackageClasses = TokenRelayTestController.class)
	@EnableOAuth2Client
	protected static class ClientConfiguration {

		@Bean
		public OAuth2RestTemplate oauth2RestTemplate(OAuth2ProtectedResourceDetails resource,
				OAuth2ClientContext oauth2Context) {
			return new OAuth2RestTemplate(resource, oauth2Context);

		}

		@Bean
		public MockRestServiceServer mockRestServiceServer(OAuth2RestTemplate template) {
			return MockRestServiceServer.createServer(template);
		}

	}

	@RestController
	@TestComponent
	protected static class TokenRelayTestController {

		@Autowired
		OAuth2RestTemplate oAuth2RestTemplate;

		@GetMapping("/token-relay")
		public String callAnotherService() {

			return oAuth2RestTemplate.getForEntity("https://example.com/test", String.class).getBody();

		}

	}

}
