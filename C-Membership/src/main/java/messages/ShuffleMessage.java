package pingpong.messages;


import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;
import pt.unl.fct.di.novasys.network.data.Host;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ShuffleMessage extends ProtoMessage {

    public final static short MSG_ID = 101;


    private final Set<Host> sample;
    private final int shuffleId;

    public ShuffleMessage(Set<Host> sample, int shuffleId) {
        super(MSG_ID);
        this.sample = sample;
        this.shuffleId = shuffleId;
    }

    public int getShuffleId() {
        return shuffleId;
    }

    public Set<Host> getSample() {
        return sample;
    }

    @Override
    public String toString() {
        return "ShuffleMessage{" +
                ", sample=" + sample +
                ", shuffleId=" + shuffleId +
                '}';
    }

    public static ISerializer<ShuffleMessage> serializer = new ISerializer<ShuffleMessage>() {
        @Override
        public void serialize(ShuffleMessage shuffleMessage, ByteBuf out) throws IOException {
            out.writeInt(shuffleMessage.shuffleId);
            out.writeInt(shuffleMessage.sample.size());
            for (Host h : shuffleMessage.sample)
                Host.serializer.serialize(h, out);
        }

        @Override
        public ShuffleMessage deserialize(ByteBuf in) throws IOException {
            int shuffleId = in.readInt();
            int size = in.readInt();
            Set<Host> subset = new HashSet<>(size, 1);
            for (int i = 0; i < size; i++)
                subset.add(Host.serializer.deserialize(in));
            return new ShuffleMessage(subset, shuffleId);
        }
    };
}
