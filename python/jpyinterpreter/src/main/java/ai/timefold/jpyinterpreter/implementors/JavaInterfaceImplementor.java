package ai.timefold.jpyinterpreter.implementors;

import ai.timefold.jpyinterpreter.PythonCompiledClass;

import org.objectweb.asm.ClassWriter;

public abstract class JavaInterfaceImplementor {
    public abstract Class<?> getInterfaceClass();

    public abstract void implement(ClassWriter classWriter, PythonCompiledClass compiledClass);

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof JavaInterfaceImplementor)) {
            return false;
        }

        if (getInterfaceClass().equals(Object.class)) {
            return getClass().equals(o.getClass());
        }
        return getInterfaceClass().equals(((JavaInterfaceImplementor) o).getInterfaceClass());
    }

    @Override
    public final int hashCode() {
        return getInterfaceClass().hashCode();
    }
}
