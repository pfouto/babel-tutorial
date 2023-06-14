package messages;


import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ShuffleReplyMessage extends ProtoMessage {

    public final static short MSG_ID = 102;

    private final Set<Host> sample;

    public ShuffleReplyMessage(Set<Host> sample) {
        super(MSG_ID);
        this.sample = sample;
    }

    public Set<Host> getSample() {
        return sample;
    }

    @Override
    public String toString() {
        return "ShuffleReplyMessage{" +
                "sample=" + sample +
                '}';
    }

    public static ISerializer<ShuffleReplyMessage> serializer = new ISerializer<ShuffleReplyMessage>() {
        @Override
        public void serialize(ShuffleReplyMessage shuffleMessage, ByteBuf out) throws IOException {
            out.writeInt(shuffleMessage.sample.size());
            for (Host h : shuffleMessage.sample)
                Host.serializer.serialize(h, out);
        }

        @Override
        public ShuffleReplyMessage deserialize(ByteBuf in) throws IOException {
            int size = in.readInt();
            Set<Host> subset = new HashSet<>(size, 1);
            for (int i = 0; i < size; i++)
                subset.add(Host.serializer.deserialize(in));
            return new ShuffleReplyMessage(subset);
        }
    };
}
