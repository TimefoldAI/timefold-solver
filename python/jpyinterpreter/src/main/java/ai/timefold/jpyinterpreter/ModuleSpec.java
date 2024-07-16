package ai.timefold.jpyinterpreter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public final class ModuleSpec {
    final PythonInteger level;
    final List<PythonString> fromList;
    final Map<String, PythonLikeObject> globalsMap;
    final Map<String, PythonLikeObject> localsMap;
    final String name;

    public ModuleSpec(PythonInteger level, List<PythonString> fromList, Map<String, PythonLikeObject> globalsMap,
            Map<String, PythonLikeObject> localsMap, String name) {
        this.level = level;
        this.fromList = fromList;
        this.globalsMap = globalsMap;
        this.localsMap = localsMap;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModuleSpec that = (ModuleSpec) o;
        return level.equals(that.level) && fromList.equals(that.fromList) && globalsMap == that.globalsMap
                && localsMap == that.localsMap && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(level, fromList, System.identityHashCode(globalsMap),
                System.identityHashCode(localsMap), name);
    }

    @Override
    public String toString() {
        return "ModuleSpec{" +
                "name='" + name + '\'' +
                ", level=" + level +
                ", fromList=" + fromList +
                ", globalsMap=" + System.identityHashCode(globalsMap) +
                ", localsMap=" + System.identityHashCode(localsMap) +
                '}';
    }
}
