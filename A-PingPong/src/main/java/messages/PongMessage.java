package messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class PongMessage extends ProtoMessage {

    public static final short MSG_ID = 102;

    private final int pingId;
    private final String message;
    public PongMessage(int pingId, String message) {
        super(MSG_ID);
        this.pingId = pingId;
        this.message = message;
    }


    public int getPingId() {
        return pingId;
    }

    public String getMessage() {
        return message;
    }

    public static ISerializer<? extends ProtoMessage> serializer = new ISerializer<PongMessage>() {
        public void serialize(PongMessage msg, ByteBuf out) {
            out.writeInt(msg.pingId);
            Utils.encodeUTF8(msg.message, out);
        }

        public PongMessage deserialize(ByteBuf in) {
            int pingId = in.readInt();
            String message = Utils.decodeUTF8(in);
            return new PongMessage(pingId, message);
        }
    };

}
