package requests;

import pt.unl.fct.di.novasys.babel.generic.ProtoRequest;

public class BroadcastRequest extends ProtoRequest {

    public static final short REQUEST_ID = 201;

    private final String msg;

    public BroadcastRequest(String msg) {
        super(REQUEST_ID);
        this.msg = msg;
    }

    public String getMsg() {
        return msg;
    }
}
