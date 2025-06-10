package ai.timefold.solver.core.impl.domain.variable.declarative;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.NoSuchElementException;

import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.preview.api.domain.variable.declarative.ShadowSources;

class PathPartIterator implements Iterator<PathPart> {
    private final Class<?> rootEntity;
    private final String[] parts;
    private final String path;
    private PathPart previous;

    public PathPartIterator(Class<?> rootEntity, String[] parts, String path) {
        this.rootEntity = rootEntity;
        this.parts = parts;
        this.path = path;
        previous = new PathPart(-1, "", null, rootEntity, rootEntity, false);
    }

    @Override
    public boolean hasNext() {
        return previous.index() < parts.length - 1;
    }

    @Override
    public PathPart next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        var index = previous.index() + 1;
        var name = parts[index];
        var isCollection = false;
        if (name.endsWith(RootVariableSource.COLLECTION_REFERENCE_SUFFIX)) {
            name = name.substring(0, name.length() - RootVariableSource.COLLECTION_REFERENCE_SUFFIX.length());
            isCollection = true;
        }
        Class<?> previousType;
        if (previous.isCollection()) {
            previousType = ConfigUtils.extractGenericTypeParameterOrFail(ShadowSources.class.getSimpleName(),
                    previous.memberType(),
                    previous.memberType(), previous.memberGenericType(), ShadowSources.class,
                    previous.name());

        } else {
            previousType = previous.memberType();
        }
        var member = RootVariableSource.getMember(rootEntity, path, previousType, name);
        Class<?> memberType;
        Type memberGenericType;
        if (member instanceof Field field) {
            memberType = field.getType();
            memberGenericType = field.getGenericType();
        } else if (member instanceof Method method) {
            memberType = method.getReturnType();
            memberGenericType = method.getGenericReturnType();
        } else {
            throw new IllegalStateException("Unsupported member type: " + member.getClass());
        }
        var out = new PathPart(index, name, member, memberType, memberGenericType, isCollection);
        previous = out;
        return out;
    }
}
