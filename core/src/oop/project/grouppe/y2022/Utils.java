package oop.project.grouppe.y2022;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;

public class Utils {
	public static float lerp(float a, float b, float w)
	{
		return a * (1.0f - w) + (b * w);
	}
	
	public static float clamp(float v, float min, float max) {
		return (v < min) ? min : ((v > max) ? max : v);
	}
	
	public static void getLocalIP(ArrayList<String[]> list) {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface iface = interfaces.nextElement();
				if (iface.isLoopback() || !iface.isUp()) continue;
				Enumeration<InetAddress> addresses = iface.getInetAddresses();
				while(addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet6Address) continue; // IPV4 ONLY
					String ip = addr.getHostAddress();
					list.add(new String[]{iface.getDisplayName(), ip});
				}
			}
		} catch (SocketException e) {
			list.clear();
		}
	}
}
