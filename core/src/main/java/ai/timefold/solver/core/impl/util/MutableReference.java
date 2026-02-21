package ai.timefold.solver.core.impl.util;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class MutableReference<Value_> {

    private @Nullable Value_ value;

    public MutableReference() {
        this(null);
    }

    public MutableReference(@Nullable Value_ value) {
        this.value = value;
    }

    public @Nullable Value_ getValue() {
        return value;
    }

    public void setValue(@Nullable Value_ value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof MutableReference<?> other) {
            return Objects.equals(value, other.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value == null ? "null" : value.toString();
    }

}
