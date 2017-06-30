package org.springframework.cloud.client.discovery.simple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.ServerProperties;
// import org.springframework.boot.context.embedded.EmbeddedServletContainer;
// import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.noop.NoopDiscoveryClientAutoConfiguration;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.ClassUtils;

import java.net.URI;

/**
 * Spring Boot Auto-Configuration for Simple Properties based Discovery Client
 *
 * @author Biju Kunjummen
 */

@Configuration
@AutoConfigureBefore(NoopDiscoveryClientAutoConfiguration.class)
public class SimpleDiscoveryClientAutoConfiguration {

	@Autowired(required = false)
	private ServerProperties server;

	@Autowired
	private ApplicationContext context;

	@Value("${spring.application.name:application}")
	private String serviceId;

	@Autowired
	private InetUtils inet;

	@Bean
	public SimpleDiscoveryProperties simpleDiscoveryProperties() {
		SimpleDiscoveryProperties simple = new SimpleDiscoveryProperties();
		simple.getLocal().setServiceId(this.serviceId);
		simple.getLocal()
				.setUri(URI.create(
						"http://" + this.inet.findFirstNonLoopbackHostInfo().getHostname()
								+ ":" + findPort()));
		return simple;
	}

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public DiscoveryClient simpleDiscoveryClient() {
		return new SimpleDiscoveryClient(simpleDiscoveryProperties());
	}

	private int findPort() {
		//FIXME: is what is the boot 2.0 equiv?
		/*if (ClassUtils.isPresent(
				"org.springframework.boot.context.embedded.EmbeddedWebApplicationContext",
				null)) {
			if (this.context instanceof EmbeddedWebApplicationContext) {
				EmbeddedServletContainer container = ((EmbeddedWebApplicationContext) this.context)
						.getEmbeddedServletContainer();
				if (container != null) {
					return container.getPort();
				}
			}
		}*/
		if (this.server != null && this.server.getPort() != null
				&& this.server.getPort() > 0) {
			return this.server.getPort();
		}
		return 8080;
	}

}
