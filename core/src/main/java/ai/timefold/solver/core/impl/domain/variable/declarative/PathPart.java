package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.lang.reflect.Member;
import java.lang.reflect.Type;

public record PathPart(int index, String name, Member member, Class<?> memberType, Type memberGenericType,
        boolean isCollection) {
}
