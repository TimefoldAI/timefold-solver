package ai.timefold.solver.core.impl.bavet.common;

import java.util.Objects;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Triple;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ConstraintNodeLocation(String className,
        String methodName,
        int lineNumber) {

    public static ConstraintNodeLocation unknown() {
        return new ConstraintNodeLocation("<unknown>", "<unknown>", -1);
    }

    public record LocationKeyAndDisplay(Object key, String display) implements Comparable<LocationKeyAndDisplay> {
        @Override
        public boolean equals(Object object) {
            if (!(object instanceof LocationKeyAndDisplay that))
                return false;
            return Objects.equals(key, that.key);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key);
        }

        @Override
        public String toString() {
            return display;
        }

        @Override
        public int compareTo(LocationKeyAndDisplay o) {
            return display.compareTo(o.display);
        }
    }

    public LocationKeyAndDisplay getMethodId() {
        return new LocationKeyAndDisplay(new Pair<>(className, methodName),
                "%s#%s".formatted(className, methodName));
    }

    public LocationKeyAndDisplay getLineId() {
        return new LocationKeyAndDisplay(new Triple<>(className, methodName, lineNumber),
                "%s#%s:%d".formatted(className, methodName, lineNumber));
    }
}
