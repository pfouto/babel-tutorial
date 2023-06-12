import notifications.DeliverNotification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.internal.InternalEvent;
import requests.BroadcastRequest;

import java.io.IOException;
import java.util.Properties;
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

        new Thread(() -> {
            try {
                inputLoop();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
            }
        }).start();
    }

    private void inputLoop() {
        while (true) {
            String msg = System.console().readLine();
            if (msg.equals("exit")) System.exit(0);
            else if (msg.equals("help")) System.out.println("Type 'exit' to exit");
            else {
                sendRequest(new BroadcastRequest(msg), FloodGossip.PROTO_ID);
            }
        }
    }

    private void uponDeliver(DeliverNotification not, short sourceProto) {
        logger.info("Received via {}: {} - {} hops", not.getVia().getPort(), not.getMsg(), not.getnHops());
    }

}
