import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pt.unl.fct.di.novasys.babel.core.Babel;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.exceptions.InvalidParameterException;
import pt.unl.fct.di.novasys.babel.exceptions.ProtocolAlreadyExistsException;

import java.io.IOException;
import java.util.Properties;

public class Main {
    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) throws InvalidParameterException, IOException, HandlerRegistrationException, ProtocolAlreadyExistsException {
        //Creates a new instance of babel
        Babel babel = Babel.getInstance();

        //Reads arguments from the command line and loads them into a Properties object
        Properties props = Babel.loadConfig(args, null);

        //Creates a new instance of the FullMembership Protocol
        FullMembership fullMembership = new FullMembership();

        //Registers the protocol in babel
        babel.registerProtocol(fullMembership);

        //Initializes the protocol
        fullMembership.init(props);

        //Starts babel
        babel.start();


    }
}

