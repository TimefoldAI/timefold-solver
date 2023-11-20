package ai.timefold.solver.core.impl.util;

import java.util.Objects;

public final class MutableLong extends Number implements Comparable<MutableLong> {

    private long value;

    public MutableLong() {
        this(0L);
    }

    public MutableLong(long value) {
        this.value = value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long increment() {
        return add(1L);
    }

    public long decrement() {
        return subtract(1L);
    }

    public long add(long addend) {
        value += addend;
        return value;
    }

    public long subtract(long subtrahend) {
        value -= subtrahend;
        return value;
    }

    @Override
    public int intValue() {
        return (int) value;
    }

    @Override
    public long longValue() {
        return value;
    }

    @Override
    public float floatValue() {
        return value;
    }

    @Override
    public double doubleValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MutableLong other) {
            return value == other.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public int compareTo(MutableLong other) {
        return Long.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

}
