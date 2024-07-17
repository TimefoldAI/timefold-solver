package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToIntBiFunction;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.opcodes.Opcode;

public final class VersionMapping {
    private final NavigableMap<PythonVersion, BiFunction<PythonBytecodeInstruction, PythonVersion, Opcode>> versionToMappingMap;

    public VersionMapping() {
        this.versionToMappingMap = new TreeMap<>();
    }

    private VersionMapping(
            NavigableMap<PythonVersion, BiFunction<PythonBytecodeInstruction, PythonVersion, Opcode>> versionToMappingMap) {
        this.versionToMappingMap = versionToMappingMap;
    }

    public static VersionMapping unimplemented() {
        return new VersionMapping();
    }

    public static VersionMapping constantMapping(Function<PythonBytecodeInstruction, Opcode> mapper) {
        return new VersionMapping()
                .map(PythonVersion.MINIMUM_PYTHON_VERSION, Objects.requireNonNull(mapper));
    }

    public static VersionMapping constantMapping(BiFunction<PythonBytecodeInstruction, PythonVersion, Opcode> mapper) {
        return new VersionMapping()
                .map(PythonVersion.MINIMUM_PYTHON_VERSION, Objects.requireNonNull(mapper));
    }

    public VersionMapping map(PythonVersion version, Function<PythonBytecodeInstruction, Opcode> mapper) {
        var mapCopy = new TreeMap<>(versionToMappingMap);
        mapCopy.put(version, (instruction, ignored) -> mapper.apply(instruction));
        return new VersionMapping(mapCopy);
    }

    public VersionMapping map(PythonVersion version, BiFunction<PythonBytecodeInstruction, PythonVersion, Opcode> mapper) {
        var mapCopy = new TreeMap<>(versionToMappingMap);
        mapCopy.put(version, mapper);
        return new VersionMapping(mapCopy);
    }

    public VersionMapping mapWithLabels(PythonVersion version,
            BiFunction<PythonBytecodeInstruction, Integer, Opcode> mapper,
            ToIntBiFunction<PythonBytecodeInstruction, PythonVersion> labelMapper) {
        var mapCopy = new TreeMap<>(versionToMappingMap);
        mapCopy.put(version,
                (instruction, actualVersion) -> mapper.apply(instruction, labelMapper.applyAsInt(instruction, actualVersion)));
        return new VersionMapping(mapCopy);
    }

    public Opcode getOpcodeForVersion(PythonBytecodeInstruction instruction,
            PythonVersion pythonVersion) {
        var mappingForVersion = versionToMappingMap.floorEntry(pythonVersion);
        if (mappingForVersion == null) {
            throw new UnsupportedOperationException(
                    "Could not find implementation for Opcode %s for Python version %s (instruction %s)"
                            .formatted(instruction.opname(), pythonVersion, instruction));
        }
        return mappingForVersion.getValue().apply(instruction, pythonVersion);
    }
}
