package ai.timefold.jpyinterpreter.util;

public final class ByteCharSequence implements CharSequence {

    private final byte[] data;
    private final int inclusiveStartIndex;
    private final int exclusiveEndIndex;

    public ByteCharSequence(byte[] data) {
        this.data = data;
        this.inclusiveStartIndex = 0;
        this.exclusiveEndIndex = data.length;
    }

    public ByteCharSequence(byte[] data, int inclusiveStartIndex, int exclusiveEndIndex) {
        this.data = data;
        this.inclusiveStartIndex = inclusiveStartIndex;
        this.exclusiveEndIndex = exclusiveEndIndex;
    }

    @Override
    public int length() {
        return exclusiveEndIndex - inclusiveStartIndex;
    }

    @Override
    public char charAt(int i) {
        return (char) (data[inclusiveStartIndex + i] & 0xFF);
    }

    @Override
    public ByteCharSequence subSequence(int from, int to) {
        return new ByteCharSequence(data,
                inclusiveStartIndex + from,
                inclusiveStartIndex + to);
    }

    @Override
    public String toString() {
        char[] chars = new char[length()];

        for (int i = 0; i < length(); i++) {
            chars[i] = charAt(i);
        }

        return String.valueOf(chars);
    }
}
