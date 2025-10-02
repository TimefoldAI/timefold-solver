package ai.timefold.solver.core.impl.domain.solution.cloner.gizmo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import io.quarkus.gizmo.ClassOutput;

record GizmoSolutionClonerClassOutput(Map<String, byte[]> classNameToBytecode) implements ClassOutput {
    @Override
    public void write(String classInternalName, byte[] byteCode) {
        classNameToBytecode.put(classInternalName.replace('/', '.'), byteCode);
        if (GizmoSolutionClonerImplementor.DEBUG) {
            Path debugRoot = Paths.get("target/timefold-solver-generated-classes");
            Path rest = Paths.get(classInternalName + ".class");
            Path destination = debugRoot.resolve(rest);

            try {
                Files.createDirectories(destination.getParent());
                Files.write(destination, byteCode);
            } catch (IOException e) {
                throw new IllegalStateException("Fail to write debug class file (%s).".formatted(destination), e);
            }
        }
    }
}
