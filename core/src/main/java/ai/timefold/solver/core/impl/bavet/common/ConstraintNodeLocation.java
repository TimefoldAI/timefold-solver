package ai.timefold.solver.core.impl.bavet.common;

import java.util.Comparator;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ConstraintNodeLocation(
        String className,
        String methodName,
        int lineNumber) implements Comparable<ConstraintNodeLocation> {

    public static ConstraintNodeLocation unknown() {
        return new ConstraintNodeLocation("<unknown>", "<unknown>", -1);
    }

    @Override
    public String toString() {
        return "%s#%s:%d".formatted(className, methodName, lineNumber);
    }

    @Override
    public int compareTo(ConstraintNodeLocation o) {
        return Comparator.comparing(ConstraintNodeLocation::className)
                .thenComparing(ConstraintNodeLocation::methodName)
                .thenComparing(ConstraintNodeLocation::lineNumber)
                .compare(this, o);
    }
}
