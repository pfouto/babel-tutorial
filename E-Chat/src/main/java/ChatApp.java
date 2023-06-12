import notifications.DeliverNotification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import pt.unl.fct.di.novasys.network.data.Host;
import requests.BroadcastRequest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;

public class ChatApp extends GenericProtocol {
    private static final Logger logger = LogManager.getLogger(ChatApp.class);

    public static final short PROTO_ID = 301;
    public static final String PROTO_NAME = "ChatApp";

    public ChatApp() {
        super(PROTO_NAME, PROTO_ID);
    }

    @Override
    public void init(Properties props) throws HandlerRegistrationException, IOException {
        subscribeNotification(DeliverNotification.NOTIFICATION_ID, this::uponDeliver);

        new Thread(this::readSystemIn).start();
    }

    private void readSystemIn() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                String line = scanner.nextLine();
                if (line == null) break;
                if (line.equals("quit")) {
                    System.exit(0);
                }
                readCommand(line);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }

    private void readCommand(String line) {
        sendRequest(new BroadcastRequest(line), FloodGossip.PROTO_ID);
    }

    private void uponDeliver(DeliverNotification not, short sourceProto) {
        logger.info("Received via {}: {} - {} hops", not.getVia().getPort(), not.getMsg(), not.getnHops());
    }

}
