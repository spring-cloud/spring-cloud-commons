package org.springframework.cloud.util;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Enumeration;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.apachecommons.CommonsLog;

/**
 * @author Spencer Gibb
 */
@CommonsLog
public class InetUtils {

	/**
	 * Find the first non-loopback host info.
	 * If there were errors return a hostinfo with 'localhost' and '127.0.0.1' for hostname and ipAddress respectively.
	 */
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
	public static InetAddress getFirstNonLoopbackAddress() {
		try {
			for (Enumeration<NetworkInterface> enumNic = NetworkInterface.getNetworkInterfaces();
				 enumNic.hasMoreElements(); ) {
				NetworkInterface ifc = enumNic.nextElement();
				if (ifc.isUp()) {
					for (Enumeration<InetAddress> enumAddr = ifc.getInetAddresses();
						 enumAddr.hasMoreElements(); ) {
						InetAddress address = enumAddr.nextElement();
						if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
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
		} catch (UnknownHostException e) {
			log.warn("Unable to retrieve localhost");
		}

		return null;
	}

	public static HostInfo convert(InetAddress address) {
		HostInfo hostInfo = new HostInfo();
		hostInfo.setHostname(address.getHostName());
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
