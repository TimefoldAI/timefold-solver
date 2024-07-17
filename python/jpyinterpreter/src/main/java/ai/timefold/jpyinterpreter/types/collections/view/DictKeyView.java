package ai.timefold.jpyinterpreter.types.collections.view;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.DelegatePythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeSet;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class DictKeyView extends AbstractPythonLikeObject {
    public final static PythonLikeType $TYPE = BuiltinTypes.DICT_KEY_VIEW_TYPE;

    final PythonLikeDict mapping;
    final Set<PythonLikeObject> keySet;

    static {
        PythonOverloadImplementor.deferDispatchesFor(DictKeyView::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Unary
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, DictKeyView.class.getMethod("getKeysSize"));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR,
                DictKeyView.class.getMethod("getKeysIterator"));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.REVERSED,
                DictKeyView.class.getMethod("getReversedKeyIterator"));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                DictKeyView.class.getMethod("toRepresentation"));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                DictKeyView.class.getMethod("toRepresentation"));

        // Binary
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.CONTAINS,
                DictKeyView.class.getMethod("containsKey", PythonLikeObject.class));

        // Set methods
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addMethod("isdisjoint", DictKeyView.class.getMethod("isDisjoint", DictKeyView.class));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.LESS_THAN_OR_EQUAL,
                DictKeyView.class.getMethod("isSubset", DictKeyView.class));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.LESS_THAN,
                DictKeyView.class.getMethod("isStrictSubset", DictKeyView.class));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.GREATER_THAN_OR_EQUAL,
                DictKeyView.class.getMethod("isSuperset", DictKeyView.class));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.GREATER_THAN,
                DictKeyView.class.getMethod("isStrictSuperset", DictKeyView.class));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.OR,
                DictKeyView.class.getMethod("union", DictKeyView.class));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.AND,
                DictKeyView.class.getMethod("intersection", DictKeyView.class));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.SUBTRACT,
                DictKeyView.class.getMethod("difference", DictKeyView.class));
        BuiltinTypes.DICT_KEY_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.XOR,
                DictKeyView.class.getMethod("symmetricDifference", DictKeyView.class));

        return BuiltinTypes.DICT_KEY_VIEW_TYPE;
    }

    public DictKeyView(PythonLikeDict mapping) {
        super(BuiltinTypes.DICT_KEY_VIEW_TYPE);
        this.mapping = mapping;
        this.keySet = mapping.delegate.keySet();
        $setAttribute("mapping", mapping);
    }

    public PythonInteger getKeysSize() {
        return PythonInteger.valueOf(keySet.size());
    }

    public DelegatePythonIterator<PythonLikeObject> getKeysIterator() {
        return new DelegatePythonIterator<>(keySet.iterator());
    }

    public PythonBoolean containsKey(PythonLikeObject key) {
        return PythonBoolean.valueOf(keySet.contains(key));
    }

    public DelegatePythonIterator<PythonLikeObject> getReversedKeyIterator() {
        return mapping.reversed();
    }

    public PythonBoolean isDisjoint(DictKeyView other) {
        return PythonBoolean.valueOf(Collections.disjoint(keySet, other.keySet));
    }

    public PythonBoolean isSubset(DictKeyView other) {
        return PythonBoolean.valueOf(other.keySet.containsAll(keySet));
    }

    public PythonBoolean isStrictSubset(DictKeyView other) {
        return PythonBoolean.valueOf(other.keySet.containsAll(keySet) && !keySet.containsAll(other.keySet));
    }

    public PythonBoolean isSuperset(DictKeyView other) {
        return PythonBoolean.valueOf(keySet.containsAll(other.keySet));
    }

    public PythonBoolean isStrictSuperset(DictKeyView other) {
        return PythonBoolean.valueOf(keySet.containsAll(other.keySet) && !other.keySet.containsAll(keySet));
    }

    public PythonLikeSet union(DictKeyView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(keySet);
        out.delegate.addAll(other.keySet);
        return out;
    }

    public PythonLikeSet intersection(DictKeyView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(keySet);
        out.delegate.retainAll(other.keySet);
        return out;
    }

    public PythonLikeSet difference(DictKeyView other) {
        PythonLikeSet out = new PythonLikeSet();
        out.delegate.addAll(keySet);
        out.delegate.removeAll(other.keySet);
        return out;
    }

    public PythonLikeSet symmetricDifference(DictKeyView other) {
        var out = new PythonLikeSet<>();
        out.delegate.addAll(keySet);
        other.keySet.stream() // for each item in other
                .filter(Predicate.not(e -> out.delegate.add(e))) // add each item
                .forEach(out.delegate::remove); // add return false iff item already in set, so this remove
        // all items in both this and other
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DictKeyView other) {
            return keySet.equals(other.keySet);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return keySet.hashCode();
    }

    public PythonString toRepresentation() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("dict_keys([");

        for (PythonLikeObject key : keySet) {
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke(key));
            out.append(", ");
        }
        out.delete(out.length() - 2, out.length());
        out.append("])");

        return out.toString();
    }
}
