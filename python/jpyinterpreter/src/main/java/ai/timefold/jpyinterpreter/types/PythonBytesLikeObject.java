package ai.timefold.jpyinterpreter.types;

import java.nio.ByteBuffer;

import ai.timefold.jpyinterpreter.PythonLikeObject;

public interface PythonBytesLikeObject extends PythonLikeObject {
    ByteBuffer asByteBuffer();

    default byte[] asByteArray() {
        ByteBuffer byteBuffer = asByteBuffer();
        byte[] out = new byte[byteBuffer.limit()];
        byteBuffer.get(out);
        return out;
    }
}
