import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InvalidParameterException, IOException, HandlerRegistrationException, ProtocolAlreadyExistsException {
        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

        //Creates a new instance of the FullMembership Protocol
        ChatApp chat = new ChatApp();
        FloodGossip flood = new FloodGossip();
        FullMembership fullMembership = new FullMembership();

        //Registers the protocol in babel
        babel.registerProtocol(chat);
        babel.registerProtocol(flood);
        babel.registerProtocol(fullMembership);

        //Initializes the protocol
        chat.init(props);
        flood.init(props);
        fullMembership.init(props);

        //Starts babel
        babel.start();
    }

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

}

