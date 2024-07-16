package ai.timefold.jpyinterpreter;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class ValueSourceInfo {
    final PythonLikeType valueType;
    final Set<Opcode> possibleSourceOpcodeSet;
    final Set<ValueSourceInfo> valueDependencySet;

    private ValueSourceInfo(PythonLikeType valueType,
            Set<Opcode> possibleSourceOpcodeSet,
            Set<ValueSourceInfo> valueDependencySet) {
        this.valueType = valueType;
        this.possibleSourceOpcodeSet = possibleSourceOpcodeSet;
        this.valueDependencySet = valueDependencySet;
    }

    public PythonLikeType getValueType() {
        return valueType;
    }

    public Set<Opcode> getPossibleSourceOpcodeSet() {
        return possibleSourceOpcodeSet;
    }

    public Set<ValueSourceInfo> getValueDependencySet() {
        return valueDependencySet;
    }

    public ValueSourceInfo unifyWith(ValueSourceInfo other) {
        PythonLikeType newValueType = valueType.unifyWith(other.valueType);

        Set<Opcode> newPossibleSourceOpcodeSet =
                new HashSet<>(possibleSourceOpcodeSet.size() + other.possibleSourceOpcodeSet.size());
        newPossibleSourceOpcodeSet.addAll(possibleSourceOpcodeSet);
        newPossibleSourceOpcodeSet.addAll(other.possibleSourceOpcodeSet);

        Set<ValueSourceInfo> newValueDependencySet = new HashSet<>(valueDependencySet.size() + other.valueDependencySet.size());
        newValueDependencySet.addAll(valueDependencySet);

        for (ValueSourceInfo dependency : other.valueDependencySet) {
            Optional<ValueSourceInfo> maybeCommonDependency = valueDependencySet.stream()
                    .filter(otherValueSource -> otherValueSource.possibleSourceOpcodeSet
                            .equals(dependency.getPossibleSourceOpcodeSet()))
                    .findAny();

            // If it is not empty, it was added in the previous loop
            if (maybeCommonDependency.isEmpty()) {
                newValueDependencySet.add(dependency);
            }
        }
        return new ValueSourceInfo(newValueType, newPossibleSourceOpcodeSet, newValueDependencySet);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ValueSourceInfo that = (ValueSourceInfo) o;
        return valueType.equals(that.valueType) && possibleSourceOpcodeSet.equals(that.possibleSourceOpcodeSet)
                && valueDependencySet.equals(that.valueDependencySet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valueType, possibleSourceOpcodeSet, valueDependencySet);
    }

    @Override
    public String toString() {
        return "ValueSourceInfo{" +
                "valueType=" + valueType +
                ", possibleSourceOpcodeList=" + possibleSourceOpcodeSet +
                ", valueDependencyList=" + valueDependencySet +
                '}';
    }

    public static ValueSourceInfo of(Opcode sourceOpcode, PythonLikeType valueType, ValueSourceInfo... dependencies) {
        return new ValueSourceInfo(valueType, Set.of(sourceOpcode), Set.of(dependencies));
    }

    public static ValueSourceInfo of(Opcode sourceOpcode, PythonLikeType valueType, List<ValueSourceInfo> dependencyList) {
        return new ValueSourceInfo(valueType, Set.of(sourceOpcode), new HashSet<>(dependencyList));
    }
}
