package org.springframework.cloud.autoconfigure;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * @author Spencer Gibb
 */
public class LifecycleMvcAutoConfigurationTests {

    // postEnvMvcEndpoint
    @Test
    public void postEnvMvcEndpointDisabled() {
        beanNotCreated("environmentManagerEndpoint",
                "endpoints.env.post.enabled=false");
    }

    @Test
    public void postEnvMvcEndpointGloballyDisabled() {
        beanNotCreated("environmentManagerEndpoint",
                "endpoints.enabled=false");
    }

    @Test
    public void postEnvMvcEndpointEnabled() {
        beanCreated("environmentManagerEndpoint",
                "endpoints.env.post.enabled=true");
    }

    // restartMvcEndpoint
    @Test
    public void restartMvcEndpointDisabled() {
        beanNotCreated("restartMvcEndpoint",
                "endpoints.restart.enabled=false");
    }

    @Test
    public void restartMvcEndpointGloballyDisabled() {
        beanNotCreated("restartMvcEndpoint",
                "endpoints.enabled=false");
    }

    @Test
    public void restartMvcEndpointEnabled() {
        beanCreatedAndEndpointEnabled("restartMvcEndpoint",
                "endpoints.restart.enabled=true");
    }

    // pauseMvcEndpoint
    @Test
    public void pauseMvcEndpointDisabled() {
        beanNotCreated("pauseMvcEndpoint",
                "endpoints.pause.enabled=false");
    }

    @Test
    public void pauseMvcEndpointRestartDisabled() {
        beanNotCreated("pauseMvcEndpoint",
                "endpoints.restart.enabled=false",
                "endpoints.pause.enabled=true");
    }

    @Test
    public void pauseMvcEndpointGloballyDisabled() {
        beanNotCreated("pauseMvcEndpoint",
                "endpoints.enabled=false");
    }

    @Test
    public void pauseMvcEndpointEnabled() {
        beanCreatedAndEndpointEnabled("pauseMvcEndpoint",
                "endpoints.restart.enabled=true",
                "endpoints.pause.enabled=true");
    }

    // resumeMvcEndpoint
    @Test
    public void resumeMvcEndpointDisabled() {
        beanNotCreated("resumeMvcEndpoint",
                "endpoints.restart.enabled=true",
                "endpoints.resume.enabled=false");
    }

    @Test
    public void resumeMvcEndpointRestartDisabled() {
        beanNotCreated("resumeMvcEndpoint",
                "endpoints.restart.enabled=false",
                "endpoints.resume.enabled=true");
    }

    @Test
    public void resumeMvcEndpointGloballyDisabled() {
        beanNotCreated("resumeMvcEndpoint",
                "endpoints.enabled=false");
    }

    @Test
    public void resumeMvcEndpointEnabled() {
        beanCreatedAndEndpointEnabled("resumeMvcEndpoint",
                "endpoints.restart.enabled=true",
                "endpoints.resume.enabled=true");
    }

    private void beanNotCreated(String beanName, String... contextProperties) {
        try (ConfigurableApplicationContext context = getApplicationContext(Config.class, contextProperties)) {
            assertThat("bean was created", context.containsBeanDefinition(beanName), equalTo(false));
        }
    }

    private void beanCreated(String beanName, String... contextProperties) {
        try (ConfigurableApplicationContext context = getApplicationContext(Config.class, contextProperties)) {
            assertThat("bean was not created", context.containsBeanDefinition(beanName), equalTo(true));
        }
    }

    private void beanCreatedAndEndpointEnabled(String beanName, String... properties) {
        try (ConfigurableApplicationContext context = getApplicationContext(Config.class, properties)) {
            assertThat("bean was not created", context.containsBeanDefinition(beanName), equalTo(true));

            EndpointMvcAdapter endpoint = context.getBean(beanName, EndpointMvcAdapter.class);
            Object result = endpoint.invoke();

            assertThat("result is wrong type", result,
                    is(not(instanceOf(ResponseEntity.class))));
        }
    }

    private static ConfigurableApplicationContext getApplicationContext(
            Class<?> configuration, String... properties) {

        List<String> defaultProperties = Lists.newArrayList(properties);
        defaultProperties.add("server.port=0");
        defaultProperties.add("spring.jmx.default-domain=${random.uuid}");

        return new SpringApplicationBuilder(configuration).properties(defaultProperties.toArray(new String[]{})).run();
    }

    @Configuration
    @EnableAutoConfiguration
    static class Config {

    }
}
