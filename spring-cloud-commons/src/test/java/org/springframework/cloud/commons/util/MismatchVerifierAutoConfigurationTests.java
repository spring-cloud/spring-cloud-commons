package org.springframework.cloud.commons.util;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class MismatchVerifierAutoConfigurationTests {

	@Autowired MyMismatchVerifier myMismatchVerifier;

	@Test
	public void contextLoads() {
		BDDAssertions.then(this.myMismatchVerifier.called).isTrue();
	}

	@Configuration
	@EnableAutoConfiguration
	static class TestConfiguration {
		@Bean
		MyMismatchVerifier myMismatchVerifier() {
			return new MyMismatchVerifier();
		}
	}

	private static class MyMismatchVerifier implements MismatchVerifier {

		boolean called;

		@Override
		public VerificationResult verify() {
			this.called = true;
			return new VerificationResult();
		}
	}
}

