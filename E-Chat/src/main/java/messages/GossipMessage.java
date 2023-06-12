package messages;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

import java.util.UUID;

public class GossipMessage extends ProtoMessage {

    public static final short MSG_ID = 201;

    private final UUID mid;
    private int round;

    private final String content;

    @Override
    public String toString() {
        return "GossipMessage{" +
                "mid=" + mid +
                ", round=" + round +
                ", content='" + content + '\'' +
                '}';
    }

    public GossipMessage(UUID mid, int round, String content) {
        super(MSG_ID);
        this.mid = mid;
        this.round = round;
        this.content = content;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public UUID getMid() {
        return mid;
    }

    public String getContent() {
        return content;
    }

    public static ISerializer<GossipMessage> serializer = new ISerializer<GossipMessage>() {
        @Override
        public void serialize(GossipMessage gossipMessage, ByteBuf out) {
            out.writeLong(gossipMessage.mid.getMostSignificantBits());
            out.writeLong(gossipMessage.mid.getLeastSignificantBits());
            out.writeInt(gossipMessage.round);
            Utils.encodeUTF8(gossipMessage.content, out);
        }

        @Override
        public GossipMessage deserialize(ByteBuf in) {
            long mostSig = in.readLong();
            long leastSig = in.readLong();
            UUID mid = new UUID(mostSig, leastSig);
            int round = in.readInt();
            String content = Utils.decodeUTF8(in);
            return new GossipMessage(mid, round, content);
        }
    };
}
