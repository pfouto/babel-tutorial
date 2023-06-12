import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.Babel;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;

public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        System.setProperty("log4j2.configurationFile", "log4j2.xml");

        Babel babel = Babel.getInstance();

        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

        //Creates a new instance of the PingPongProtocol
        PingPongProtocol pingPong = new PingPongProtocol();

        //Registers the protocol in babel
        babel.registerProtocol(pingPong);

        //Initializes the protocol
        pingPong.init(props);

        //Starts babel
        babel.start();
    }

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