package org.springframework.cloud.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.springframework.util.SystemPropertyUtils;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class InetUtils {

	private final ExecutorService executorService;
	private final InetUtilsProperties properties;

	public InetUtils(final InetUtilsProperties properties) {
		this.properties = properties;
		this.executorService = Executors
				.newSingleThreadExecutor(new ThreadFactory() {
					@Override
					public Thread newThread(Runnable r) {
						Thread thread = new Thread(r);
						thread.setName(properties.getExecutorThreadName());
						thread.setDaemon(true);
						return thread;
					}
				});
	}

	public HostInfo findFirstNonLoopbackHostInfo() {
		InetAddress address = findFirstNonLoopbackAddress();
		if (address != null) {
			return convertAddress(address);
		}
		HostInfo hostInfo = new HostInfo();
		hostInfo.setHostname(this.properties.getDefaultHostname());
		hostInfo.setIpAddress(this.properties.getDefaultIpAddress());
		return hostInfo;
	}

	public InetAddress findFirstNonLoopbackAddress() {
		try {
			for (Enumeration<NetworkInterface> nics = NetworkInterface
					.getNetworkInterfaces(); nics.hasMoreElements();) {
				NetworkInterface ifc = nics.nextElement();
				if (ifc.isUp()) {
					log.debug("Testing interface: " + ifc.getDisplayName());

					// @formatter:off
					if (!ignoreInterface(ifc.getDisplayName())) {
						for (Enumeration<InetAddress> addrs = ifc .getInetAddresses(); addrs.hasMoreElements(); ) {
							InetAddress address = addrs.nextElement();
							if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
								log.debug("Found non-loopback interface: " + ifc.getDisplayName());
								return address;
							}
						}
					}
					// @formatter:on
				}
			}
		}
		catch (IOException ex) {
			log.error("Cannot get first non-loopback address", ex);
		}

		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException e) {
			log.warn("Unable to retrieve localhost");
		}

		return null;
	}

	boolean ignoreInterface(String interfaceName) {
		for (String regex : this.properties.getIgnoredInterfaces()) {
			if (interfaceName.matches(regex)) {
				log.debug("Ignoring interface: " + interfaceName);
				return true;
			}
		}
		return false;
	}

	public HostInfo convertAddress(final InetAddress address) {
		HostInfo hostInfo = new HostInfo();
		Future<String> result = this.executorService.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return address.getHostName();
			}
		});

		String hostname;
		try {
			hostname = result.get(this.properties.getTimeoutSeconds(), TimeUnit.SECONDS);
		}
		catch (Exception e) {
			log.info("Cannot determine local hostname");
			hostname = "localhost";
		}
		hostInfo.setHostname(hostname);
		hostInfo.setIpAddress(address.getHostAddress());
		return hostInfo;
	}

	@Deprecated
	private static ExecutorService executor = Executors
			.newSingleThreadExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable r) {
					Thread thread = new Thread(r);
					thread.setName("spring.cloud.inetutils");
					thread.setDaemon(true);
					return thread;
				}
			});

	/**
	 * Find the first non-loopback host info. If there were errors return a hostinfo with
	 * 'localhost' and '127.0.0.1' for hostname and ipAddress respectively.
	 */
	@Deprecated
	public static HostInfo getFirstNonLoopbackHostInfo() {
		InetAddress address = getFirstNonLoopbackAddress();
		if (address != null) {
			return convert(address);
		}
		HostInfo hostInfo = new HostInfo();
		hostInfo.setHostname("localhost");
		hostInfo.setIpAddress("127.0.0.1");
		return hostInfo;
	}

	/**
	 * Find the first non-loopback InetAddress
	 */
	@SneakyThrows
	@Deprecated
	public static InetAddress getFirstNonLoopbackAddress() {
		try {
			for (Enumeration<NetworkInterface> enumNic = NetworkInterface
					.getNetworkInterfaces(); enumNic.hasMoreElements();) {
				NetworkInterface ifc = enumNic.nextElement();
				if (ifc.isUp()) {
					log.trace("Testing interface: " + ifc.getDisplayName());
					for (Enumeration<InetAddress> enumAddr = ifc
							.getInetAddresses(); enumAddr.hasMoreElements();) {
						InetAddress address = enumAddr.nextElement();
						if (address instanceof Inet4Address
								&& !address.isLoopbackAddress()) {
							log.trace("Found non-loopback interface: "
									+ ifc.getDisplayName());
							return address;
						}
					}
				}
			}
		}
		catch (IOException ex) {
			log.error("Cannot get first non-loopback address", ex);
		}

		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException e) {
			log.warn("Unable to retrieve localhost");
		}

		return null;
	}

	@Deprecated
	public static HostInfo convert(final InetAddress address) {
		HostInfo hostInfo = new HostInfo();
		Future<String> result = executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return address.getHostName();
			}
		});

		String hostname;
		try {
			String value = SystemPropertyUtils.resolvePlaceholders(
					"${spring.util.timeout.sec:${SPRING_UTIL_TIMEOUT_SEC:1}}");
			int timeout = 1;
			try {
				timeout = Integer.valueOf(value);
			}
			catch (NumberFormatException e) {
			}
			hostname = result.get(timeout, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			log.info("Cannot determine local hostname");
			hostname = "localhost";
		}
		hostInfo.setHostname(hostname);
		hostInfo.setIpAddress(address.getHostAddress());
		return hostInfo;
	}

	@Data
	public static final class HostInfo {
		public boolean override;
		private String ipAddress;
		private String hostname;

		public int getIpAddressAsInt() {
			InetAddress inetAddress = null;
			try {
				inetAddress = InetAddress.getByName(this.ipAddress);
			}
			catch (final UnknownHostException e) {
				throw new IllegalArgumentException(e);
			}
			return ByteBuffer.wrap(inetAddress.getAddress()).getInt();
		}
	}
}
