package org.springframework.cloud.context.environment;

import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.core.env.Environment;

/**
 * An extension of the standard {@link EnvironmentEndpoint} that allows to modify the
 * environment at runtime.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class WritableEnvironmentEndpoint extends EnvironmentEndpoint {

	public WritableEnvironmentEndpoint(Environment environment) {
		super(environment);
	}

}
