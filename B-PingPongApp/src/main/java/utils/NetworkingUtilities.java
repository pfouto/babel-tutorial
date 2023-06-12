package utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.network.data.Host;

import java.net.*;
import java.util.Enumeration;

public class NetworkingUtilities {

    private static final Logger logger = LogManager.getLogger(NetworkingUtilities.class);

    /**
     * Returns the ipv4 address of the given interface
     * @param inter name of the interface
     * @return ipv4 address of the interface
     * @throws SocketException if the interface does not exist or does not have an ipv4 address
     */
    public static String getAddress(String inter) throws SocketException {
        NetworkInterface byName = NetworkInterface.getByName(inter);
        if (byName == null) {
            logger.error("No interface named " + inter);
            return null;
        }
        Enumeration<InetAddress> addresses = byName.getInetAddresses();
        InetAddress currentAddress;
        while (addresses.hasMoreElements()) {
            currentAddress = addresses.nextElement();
            if (currentAddress instanceof Inet4Address)
                return currentAddress.getHostAddress();
        }
        logger.error("No ipv4 found for interface " + inter);
        return null;
    }

    public static Host parseHost(String s) throws UnknownHostException {
        String[] addr = s.split(":");
        return new Host(InetAddress.getByName(addr[0]), Integer.parseInt(addr[1]));
    }
}
