package app;

import pingpong.PingPongProtocol;
import pingpong.requests.PingRequest;
import pingpong.requests.PongReply;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.NetworkingUtilities;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;

public class App extends GenericProtocol {
    private PingRequest pingRequest = null;
    private int received = 0;

    public App(String protoName, short protoId) {
        super(protoName, protoId);
    }

    @Override
    public void init(Properties properties) throws HandlerRegistrationException, IOException {

        // register protocol handlers
        // register reply handler
        registerReplyHandler(PongReply.REPLY_ID, this::onPongReply);

        readSystemIn();
    }

    /**
     * Handle Pong Reply
     *
     * @param pongReply pong reply message
     * @param sourceProto source protocol
     */
    private void onPongReply(PongReply pongReply, short sourceProto) {
        System.out.println("Received reply from " + pongReply.getDestination() + " in " + pongReply.getRTT() + "ms");
        received++;
        if (received == pingRequest.getNPings()) {
            readSystemIn();
        }
    }

    /**
     * Reads commands from stdin
     */
    private void readSystemIn() {
        Scanner in = new Scanner(System.in);
        try {
            String line = in.nextLine();
            if (line == null) {
                System.exit(0);
            }
            if (line.equals("quit")) {
                System.exit(0);
            }
            readCommand(line);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a command from stdin and executes it
     *
     * @param line command to execute
     */
    private void readCommand(String line) throws UnknownHostException, InterruptedException {
        StringTokenizer tokenizer = new StringTokenizer(line);
        String cmd = tokenizer.nextToken();
        switch (cmd) {
            case "ping":
                Host destination = NetworkingUtilities.parseHost(tokenizer.nextToken());
                String message = tokenizer.nextToken();
                received = 0;
                int nPings = 1;
                if (tokenizer.hasMoreTokens()) {
                    nPings = Integer.parseInt(tokenizer.nextToken());
                }
                pingRequest = new PingRequest(message, destination, nPings);
                sendRequest(pingRequest, PingPongProtocol.PROTO_ID);
                break;
            default:
                System.out.println("Unknown command: " + cmd);
                break;
        }
    }
}
