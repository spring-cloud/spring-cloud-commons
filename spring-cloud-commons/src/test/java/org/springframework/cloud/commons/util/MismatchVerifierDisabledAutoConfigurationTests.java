package org.springframework.cloud.commons.util;

import org.assertj.core.api.BDDAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Marcin Grzejszczak
 */
@RunWith(SpringRunner.class)
@SpringBootTest(properties = "spring.cloud.util.mismatch-verifier.enabled=false")
public class MismatchVerifierDisabledAutoConfigurationTests {

	@Autowired(required = false) CompositeMismatchVerifier compositeMismatchVerifier;
	
	@Test
	public void contextLoads() {
		BDDAssertions.then(this.compositeMismatchVerifier).isNull();
	}

	@Configuration
	@EnableAutoConfiguration
	static class TestConfiguration {
	}
}

