package ai.timefold.jpyinterpreter.util;

import java.util.Objects;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

/**
 * A ClassWriter with a custom getCommonSuperClass, preventing
 * TypeNotPresent errors when computing frames.
 */
public class JavaPythonClassWriter extends ClassWriter {

    public JavaPythonClassWriter(int flags) {
        super(flags);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (Objects.equals(type1, type2)) {
            return type1;
        }

        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (TypeNotPresentException e) {
            return Type.getInternalName(Object.class);
        }
    }
}
