package pingpong.messages;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;


public class Utils {
    public static void encodeUTF8(String message, ByteBuf out) {
        byte[] stringBytes = message.getBytes(StandardCharsets.UTF_8);
        out.writeInt(stringBytes.length);
        out.writeBytes(stringBytes);
    }

    public static String decodeUTF8(ByteBuf buff) {
        byte[] stringBytes = new byte[buff.readInt()];
        buff.readBytes(stringBytes);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }


}
