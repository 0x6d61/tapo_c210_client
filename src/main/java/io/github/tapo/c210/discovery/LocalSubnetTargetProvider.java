package io.github.tapo.c210.discovery;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Finds IPv4 hosts on the local interfaces for a unicast discovery fallback. */
final class LocalSubnetTargetProvider {
    private static final int DISCOVERY_PORT = 3702;
    private static final int MAX_HOSTS_PER_SUBNET = 1024;

    private LocalSubnetTargetProvider() {
    }

    static List<InetSocketAddress> targets() throws IOException {
        var targets = new LinkedHashSet<InetSocketAddress>();
        var interfaces = NetworkInterface.getNetworkInterfaces();
        if (interfaces == null) {
            return List.of();
        }
        var allInterfaces = new ArrayList<NetworkInterface>();
        while (interfaces.hasMoreElements()) {
            allInterfaces.add(interfaces.nextElement());
        }

        var preferredInterface = preferredInterface();
        if (preferredInterface != null) {
            addInterfaceTargets(targets, preferredInterface);
        }
        for (var networkInterface : allInterfaces) {
            if (networkInterface.equals(preferredInterface)) {
                continue;
            }
            addInterfaceTargets(targets, networkInterface);
        }
        return List.copyOf(targets);
    }

    private static void addInterfaceTargets(
            Set<InetSocketAddress> targets, NetworkInterface networkInterface) throws IOException {
        if (!isUsable(networkInterface)) {
            return;
        }
        for (var address : networkInterface.getInterfaceAddresses()) {
            addSubnetTargets(targets, address);
        }
    }

    private static NetworkInterface preferredInterface() throws IOException {
        try (var socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByAddress(new byte[] {8, 8, 8, 8}), 53);
            var localAddress = socket.getLocalAddress();
            if (localAddress.isAnyLocalAddress()) {
                return null;
            }
            return NetworkInterface.getByInetAddress(localAddress);
        }
    }

    private static void addSubnetTargets(
            Set<InetSocketAddress> targets, InterfaceAddress interfaceAddress) {
        var address = interfaceAddress.getAddress();
        var prefixLength = interfaceAddress.getNetworkPrefixLength();
        if (!(address instanceof Inet4Address)
                || address.isLoopbackAddress()
                || prefixLength < 1
                || prefixLength > 30) {
            return;
        }

        var hostBits = 32 - prefixLength;
        var hostCount = (1L << hostBits) - 2;
        if (hostCount > MAX_HOSTS_PER_SUBNET) {
            return;
        }

        var localAddress = toUnsignedInt(address);
        var mask = 0xFFFFFFFFL << hostBits;
        var network = localAddress & mask;
        var broadcast = network + (1L << hostBits) - 1;
        for (var candidate = network + 1; candidate < broadcast; candidate++) {
            if (candidate != localAddress) {
                targets.add(new InetSocketAddress(fromUnsignedInt(candidate), DISCOVERY_PORT));
            }
        }
    }

    private static boolean isUsable(NetworkInterface networkInterface) throws IOException {
        return networkInterface.isUp()
                && !networkInterface.isLoopback()
                && !networkInterface.isVirtual();
    }

    private static long toUnsignedInt(InetAddress address) {
        var bytes = address.getAddress();
        return ((bytes[0] & 0xFFL) << 24)
                | ((bytes[1] & 0xFFL) << 16)
                | ((bytes[2] & 0xFFL) << 8)
                | (bytes[3] & 0xFFL);
    }

    private static String fromUnsignedInt(long value) {
        return "%d.%d.%d.%d".formatted(
                (value >>> 24) & 0xFF,
                (value >>> 16) & 0xFF,
                (value >>> 8) & 0xFF,
                value & 0xFF);
    }
}
