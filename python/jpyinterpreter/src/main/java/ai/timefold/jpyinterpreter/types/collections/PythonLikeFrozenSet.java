package ai.timefold.jpyinterpreter.types.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
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
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

// issubclass(set, frozenset) and issubclass(frozenset, set) are both False in Python
public class PythonLikeFrozenSet extends AbstractPythonLikeObject implements Set<PythonLikeObject> {
    public final Set<PythonLikeObject> delegate;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeFrozenSet::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        BuiltinTypes.FROZEN_SET_TYPE.setConstructor((positionalArguments, namedArguments, callerInstance) -> {
            if (positionalArguments.size() == 0) {
                return new PythonLikeFrozenSet();
            } else if (positionalArguments.size() == 1) {
                return new PythonLikeFrozenSet(positionalArguments.get(0));
            } else {
                throw new ValueError("frozenset expects 0 or 1 arguments, got " + positionalArguments.size());
            }
        });

        // Unary
        BuiltinTypes.FROZEN_SET_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH,
                PythonLikeFrozenSet.class.getMethod("getLength"));
        BuiltinTypes.FROZEN_SET_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR,
                PythonLikeFrozenSet.class.getMethod("getIterator"));

        // Binary
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.CONTAINS,
                PythonLikeFrozenSet.class.getMethod("containsItem", PythonLikeObject.class));

        // Other
        BuiltinTypes.FROZEN_SET_TYPE.addMethod("isdisjoint",
                PythonLikeFrozenSet.class.getMethod("isDisjoint", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addMethod("isdisjoint",
                PythonLikeFrozenSet.class.getMethod("isDisjoint", PythonLikeFrozenSet.class));

        BuiltinTypes.FROZEN_SET_TYPE.addMethod("issubset",
                PythonLikeFrozenSet.class.getMethod("isSubset", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.LESS_THAN_OR_EQUAL,
                PythonLikeFrozenSet.class.getMethod("isSubset", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.LESS_THAN,
                PythonLikeFrozenSet.class.getMethod("isStrictSubset", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addMethod("issubset",
                PythonLikeFrozenSet.class.getMethod("isSubset", PythonLikeFrozenSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.LESS_THAN_OR_EQUAL,
                PythonLikeFrozenSet.class.getMethod("isSubset", PythonLikeFrozenSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.LESS_THAN,
                PythonLikeFrozenSet.class.getMethod("isStrictSubset", PythonLikeFrozenSet.class));

        BuiltinTypes.FROZEN_SET_TYPE.addMethod("issuperset",
                PythonLikeFrozenSet.class.getMethod("isSuperset", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.GREATER_THAN_OR_EQUAL,
                PythonLikeFrozenSet.class.getMethod("isSuperset", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.GREATER_THAN,
                PythonLikeFrozenSet.class.getMethod("isStrictSuperset", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addMethod("issuperset",
                PythonLikeFrozenSet.class.getMethod("isSuperset", PythonLikeFrozenSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.GREATER_THAN_OR_EQUAL,
                PythonLikeFrozenSet.class.getMethod("isSuperset", PythonLikeFrozenSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.GREATER_THAN,
                PythonLikeFrozenSet.class.getMethod("isStrictSuperset", PythonLikeFrozenSet.class));

        BuiltinTypes.FROZEN_SET_TYPE.addMethod("union", PythonLikeFrozenSet.class.getMethod("union", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addMethod("union",
                PythonLikeFrozenSet.class.getMethod("union", PythonLikeFrozenSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.OR,
                PythonLikeFrozenSet.class.getMethod("union", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.OR,
                PythonLikeFrozenSet.class.getMethod("union", PythonLikeFrozenSet.class));

        BuiltinTypes.FROZEN_SET_TYPE.addMethod("intersection",
                PythonLikeFrozenSet.class.getMethod("intersection", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addMethod("intersection",
                PythonLikeFrozenSet.class.getMethod("intersection", PythonLikeFrozenSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.AND,
                PythonLikeFrozenSet.class.getMethod("intersection", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.AND,
                PythonLikeFrozenSet.class.getMethod("intersection", PythonLikeFrozenSet.class));

        BuiltinTypes.FROZEN_SET_TYPE.addMethod("difference",
                PythonLikeFrozenSet.class.getMethod("difference", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addMethod("difference",
                PythonLikeFrozenSet.class.getMethod("difference", PythonLikeFrozenSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.SUBTRACT,
                PythonLikeFrozenSet.class.getMethod("difference", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.SUBTRACT,
                PythonLikeFrozenSet.class.getMethod("difference", PythonLikeFrozenSet.class));

        BuiltinTypes.FROZEN_SET_TYPE.addMethod("symmetric_difference",
                PythonLikeFrozenSet.class.getMethod("symmetricDifference", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addMethod("symmetric_difference",
                PythonLikeFrozenSet.class.getMethod("symmetricDifference", PythonLikeFrozenSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.XOR,
                PythonLikeFrozenSet.class.getMethod("symmetricDifference", PythonLikeSet.class));
        BuiltinTypes.FROZEN_SET_TYPE.addBinaryMethod(PythonBinaryOperator.XOR,
                PythonLikeFrozenSet.class.getMethod("symmetricDifference", PythonLikeFrozenSet.class));

        BuiltinTypes.FROZEN_SET_TYPE.addMethod("copy", PythonLikeFrozenSet.class.getMethod("copy"));

        return BuiltinTypes.FROZEN_SET_TYPE;
    }

    private static UnsupportedOperationException modificationError() {
        return new UnsupportedOperationException("frozenset cannot be modified once created");
    }

    public PythonLikeFrozenSet() {
        super(BuiltinTypes.FROZEN_SET_TYPE);
        delegate = new HashSet<>();
    }

    public PythonLikeFrozenSet(PythonLikeObject iterable) {
        super(BuiltinTypes.FROZEN_SET_TYPE);
        Iterator<PythonLikeObject> iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(iterable);
        delegate = new HashSet<>();
        iterator.forEachRemaining(delegate::add);
    }

    // Required for bytecode generation
    @SuppressWarnings("unused")
    public void reverseAdd(PythonLikeObject item) {
        delegate.add(item);
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(delegate.size());
    }

    public PythonBoolean containsItem(PythonLikeObject query) {
        return PythonBoolean.valueOf(delegate.contains(query));
    }

    public DelegatePythonIterator getIterator() {
        return new DelegatePythonIterator(delegate.iterator());
    }

    public PythonBoolean isDisjoint(PythonLikeSet other) {
        return PythonBoolean.valueOf(Collections.disjoint(delegate, other.delegate));
    }

    public PythonBoolean isDisjoint(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(Collections.disjoint(delegate, other.delegate));
    }

    public PythonBoolean isSubset(PythonLikeSet other) {
        return PythonBoolean.valueOf(other.delegate.containsAll(delegate));
    }

    public PythonBoolean isSubset(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(other.delegate.containsAll(delegate));
    }

    public PythonBoolean isStrictSubset(PythonLikeSet other) {
        return PythonBoolean.valueOf(other.delegate.containsAll(delegate) && !delegate.containsAll(other.delegate));
    }

    public PythonBoolean isStrictSubset(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(other.delegate.containsAll(delegate) && !delegate.containsAll(other.delegate));
    }

    public PythonBoolean isSuperset(PythonLikeSet other) {
        return PythonBoolean.valueOf(delegate.containsAll(other.delegate));
    }

    public PythonBoolean isSuperset(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(delegate.containsAll(other.delegate));
    }

    public PythonBoolean isStrictSuperset(PythonLikeSet other) {
        return PythonBoolean.valueOf(delegate.containsAll(other.delegate) && !other.delegate.containsAll(delegate));
    }

    public PythonBoolean isStrictSuperset(PythonLikeFrozenSet other) {
        return PythonBoolean.valueOf(delegate.containsAll(other.delegate) && !other.delegate.containsAll(delegate));
    }

    public PythonLikeFrozenSet union(PythonLikeSet other) {
        PythonLikeFrozenSet out = new PythonLikeFrozenSet();
        out.delegate.addAll(delegate);
        out.delegate.addAll(other.delegate);
        return out;
    }

    public PythonLikeFrozenSet union(PythonLikeFrozenSet other) {
        PythonLikeFrozenSet out = new PythonLikeFrozenSet();
        out.delegate.addAll(delegate);
        out.delegate.addAll(other.delegate);
        return out;
    }

    public PythonLikeFrozenSet intersection(PythonLikeSet other) {
        PythonLikeFrozenSet out = new PythonLikeFrozenSet();
        out.delegate.addAll(delegate);
        out.delegate.retainAll(other.delegate);
        return out;
    }

    public PythonLikeFrozenSet intersection(PythonLikeFrozenSet other) {
        PythonLikeFrozenSet out = new PythonLikeFrozenSet();
        out.delegate.addAll(delegate);
        out.delegate.retainAll(other.delegate);
        return out;
    }

    public PythonLikeFrozenSet difference(PythonLikeSet other) {
        PythonLikeFrozenSet out = new PythonLikeFrozenSet();
        out.delegate.addAll(delegate);
        out.delegate.removeAll(other.delegate);
        return out;
    }

    public PythonLikeFrozenSet difference(PythonLikeFrozenSet other) {
        PythonLikeFrozenSet out = new PythonLikeFrozenSet();
        out.delegate.addAll(delegate);
        out.delegate.removeAll(other.delegate);
        return out;
    }

    public PythonLikeFrozenSet symmetricDifference(PythonLikeSet other) {
        PythonLikeFrozenSet out = new PythonLikeFrozenSet();
        out.delegate.addAll(delegate);
        other.delegate.stream() // for each item in other
                .filter(Predicate.not(out.delegate::add)) // add each item
                .forEach(out.delegate::remove); // add return false iff item already in set, so this remove
        // all items in both this and other
        return out;
    }

    public PythonLikeFrozenSet symmetricDifference(PythonLikeFrozenSet other) {
        PythonLikeFrozenSet out = new PythonLikeFrozenSet();
        out.delegate.addAll(delegate);
        other.delegate.stream() // for each item in other
                .filter(Predicate.not(out.delegate::add)) // add each item
                .forEach(out.delegate::remove); // add return false iff item already in set, so this remove
        // all items in both this and other
        return out;
    }

    public PythonLikeFrozenSet copy() {
        return this; // frozenset are immutable, thus copy return self to duplicate behavior in Python
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public Iterator<PythonLikeObject> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return delegate.toArray(ts);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return delegate.containsAll(collection);
    }

    @Override
    public boolean add(PythonLikeObject pythonLikeObject) {
        throw modificationError();
    }

    @Override
    public boolean remove(Object o) {
        throw modificationError();
    }

    @Override
    public boolean addAll(Collection<? extends PythonLikeObject> collection) {
        throw modificationError();
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw modificationError();
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw modificationError();
    }

    @Override
    public void clear() {
        throw modificationError();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Set) {
            Set other = (Set) o;
            if (other.size() != this.size()) {
                return false;
            }
            return containsAll(other) && other.containsAll(this);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
