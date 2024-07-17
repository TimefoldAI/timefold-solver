package ai.timefold.jpyinterpreter.types.collections;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeComparable;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonSlice;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.errors.lookup.IndexError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningCloneable;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

public class PythonLikeTuple<T extends PythonLikeObject> extends AbstractPythonLikeObject implements List<T>,
        PlanningCloneable<PythonLikeTuple<T>>,
        PythonLikeComparable<PythonLikeTuple>,
        PlanningImmutable,
        RandomAccess {
    public static PythonLikeTuple EMPTY = PythonLikeTuple.fromList(Collections.emptyList());

    final List delegate;
    private int remainderToAdd;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeTuple::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        BuiltinTypes.TUPLE_TYPE.setConstructor((positionalArguments, namedArguments, callerInstance) -> {
            if (positionalArguments.isEmpty()) {
                return new PythonLikeTuple();
            } else if (positionalArguments.size() == 1) {
                PythonLikeTuple out = new PythonLikeTuple();
                PythonLikeObject iterable = positionalArguments.get(0);
                if (iterable instanceof Collection) {
                    out.delegate.addAll((Collection<? extends PythonLikeObject>) iterable);
                } else {
                    Iterator<PythonLikeObject> iterator =
                            (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(iterable);
                    iterator.forEachRemaining(out.delegate::add);
                }
                return out;
            } else {
                throw new ValueError("tuple takes 0 or 1 arguments, got " + positionalArguments.size());
            }
        });
        // Unary
        BuiltinTypes.TUPLE_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, PythonLikeTuple.class.getMethod("getLength"));
        BuiltinTypes.TUPLE_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR, PythonLikeTuple.class.getMethod("getIterator"));

        // Binary
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperator.ADD,
                PythonLikeTuple.class.getMethod("concatToNew", PythonLikeTuple.class));
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperator.MULTIPLY,
                PythonLikeTuple.class.getMethod("multiplyToNew", PythonInteger.class));
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonLikeTuple.class.getMethod("getItem", PythonInteger.class));
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonLikeTuple.class.getMethod("getSlice", PythonSlice.class));
        BuiltinTypes.TUPLE_TYPE.addBinaryMethod(PythonBinaryOperator.CONTAINS,
                PythonLikeTuple.class.getMethod("containsItem", PythonLikeObject.class));

        // Comparisons
        PythonLikeComparable.setup(BuiltinTypes.TUPLE_TYPE);

        // Other
        BuiltinTypes.TUPLE_TYPE.addMethod("index", PythonLikeTuple.class.getMethod("index", PythonLikeObject.class));
        BuiltinTypes.TUPLE_TYPE.addMethod("index",
                PythonLikeTuple.class.getMethod("index", PythonLikeObject.class, PythonInteger.class));
        BuiltinTypes.TUPLE_TYPE.addMethod("index",
                PythonLikeTuple.class.getMethod("index", PythonLikeObject.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.TUPLE_TYPE.addMethod("count", PythonLikeTuple.class.getMethod("count", PythonLikeObject.class));

        return BuiltinTypes.TUPLE_TYPE;
    }

    public PythonLikeTuple() {
        super(BuiltinTypes.TUPLE_TYPE);
        delegate = new ArrayList<>();
        remainderToAdd = 0;
    }

    public PythonLikeTuple(int size) {
        super(BuiltinTypes.TUPLE_TYPE);
        delegate = new ArrayList<>(size);
        remainderToAdd = size;
        for (int i = 0; i < size; i++) {
            delegate.add(null);
        }
    }

    @Override
    public PythonLikeTuple<T> createNewInstance() {
        return new PythonLikeTuple<>();
    }

    public static <T extends PythonLikeObject> PythonLikeTuple<T> fromItems(T... items) {
        PythonLikeTuple<T> result = new PythonLikeTuple<>();
        Collections.addAll(result, items);
        return result;
    }

    public static <T extends PythonLikeObject> PythonLikeTuple<T> fromList(List<T> other) {
        PythonLikeTuple<T> result = new PythonLikeTuple<>();
        result.addAll(other);
        return result;
    }

    public PythonLikeTuple concatToNew(PythonLikeTuple other) {
        if (delegate.isEmpty()) {
            return other;
        } else if (other.delegate.isEmpty()) {
            return this;
        }

        PythonLikeTuple result = new PythonLikeTuple();
        result.addAll(delegate);
        result.addAll(other);
        return result;
    }

    public PythonLikeTuple multiplyToNew(PythonInteger times) {
        if (times.value.compareTo(BigInteger.ZERO) <= 0) {
            if (delegate.isEmpty()) {
                return this;
            }
            return new PythonLikeTuple();
        }

        if (times.value.equals(BigInteger.ONE)) {
            return this;
        }

        PythonLikeTuple result = new PythonLikeTuple();
        int timesAsInt = times.value.intValueExact();

        for (int i = 0; i < timesAsInt; i++) {
            result.addAll(delegate);
        }

        return result;
    }

    public PythonInteger getLength() {
        return PythonInteger.valueOf(delegate.size());
    }

    public PythonBoolean containsItem(PythonLikeObject item) {
        return PythonBoolean.valueOf(delegate.contains(item));
    }

    public DelegatePythonIterator<T> getIterator() {
        return new DelegatePythonIterator<T>(delegate.iterator());
    }

    public DelegatePythonIterator getReversedIterator() {

        final ListIterator<PythonLikeObject> listIterator = delegate.listIterator(delegate.size());
        return new DelegatePythonIterator<>(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return listIterator.hasPrevious();
            }

            @Override
            public Object next() {
                return listIterator.previous();
            }
        });
    }

    public PythonLikeObject getItem(PythonInteger index) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            indexAsInt = delegate.size() + index.value.intValueExact();
        }

        if (indexAsInt < 0 || indexAsInt >= delegate.size()) {
            throw new IndexError("list index out of range");
        }

        return (PythonLikeObject) delegate.get(indexAsInt);
    }

    public PythonLikeTuple getSlice(PythonSlice slice) {
        int length = delegate.size();

        PythonLikeTuple out = new PythonLikeTuple();

        slice.iterate(length, (i, processed) -> {
            out.add((PythonLikeObject) delegate.get(i));
        });

        return out;
    }

    public PythonInteger count(PythonLikeObject search) {
        long count = 0;
        for (var x : delegate) {
            if (Objects.equals(search, x)) {
                count++;
            }
        }
        return new PythonInteger(count);
    }

    public PythonInteger index(PythonLikeObject item) {
        int result = delegate.indexOf(item);

        if (result != -1) {
            return PythonInteger.valueOf(result);
        } else {
            throw new ValueError(item + " is not in list");
        }
    }

    public PythonInteger index(PythonLikeObject item, PythonInteger start) {
        int startAsInt = start.value.intValueExact();
        if (startAsInt < 0) {
            startAsInt = delegate.size() + startAsInt;
        }

        List<PythonLikeObject> searchList = delegate.subList(startAsInt, delegate.size());
        int result = searchList.indexOf(item);
        if (result != -1) {
            return PythonInteger.valueOf(startAsInt + result);
        } else {
            throw new ValueError(item + " is not in list");
        }
    }

    public PythonInteger index(PythonLikeObject item, PythonInteger start, PythonInteger end) {
        int startAsInt = start.value.intValueExact();
        int endAsInt = end.value.intValueExact();

        if (startAsInt < 0) {
            startAsInt = delegate.size() + startAsInt;
        }

        if (endAsInt < 0) {
            endAsInt = delegate.size() + endAsInt;
        }

        List<PythonLikeObject> searchList = delegate.subList(startAsInt, endAsInt);
        int result = searchList.indexOf(item);
        if (result != -1) {
            return PythonInteger.valueOf(startAsInt + result);
        } else {
            throw new ValueError(item + " is not in list");
        }
    }

    public void reverseAdd(PythonLikeObject object) {
        delegate.set(remainderToAdd - 1, object);
        remainderToAdd--;
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
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return (T[]) delegate.toArray(ts);
    }

    @Override
    public boolean add(PythonLikeObject pythonLikeObject) {
        return delegate.add(pythonLikeObject);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return delegate.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        return delegate.addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> collection) {
        return delegate.addAll(i, collection);
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        return delegate.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        return delegate.retainAll(collection);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public T get(int i) {
        return (T) delegate.get(i);
    }

    @Override
    public T set(int i, T pythonLikeObject) {
        return (T) delegate.set(i, pythonLikeObject);
    }

    @Override
    public void add(int i, PythonLikeObject pythonLikeObject) {
        delegate.add(i, pythonLikeObject);
    }

    @Override
    public T remove(int i) {
        return (T) delegate.remove(i);
    }

    @Override
    public int indexOf(Object o) {
        return delegate.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return delegate.lastIndexOf(o);
    }

    @Override
    public ListIterator<T> listIterator() {
        return delegate.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        return delegate.listIterator(i);
    }

    @Override
    public List<T> subList(int i, int i1) {
        return delegate.subList(i, i1);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof List) {
            List other = (List) o;
            if (other.size() != this.size()) {
                return false;
            }
            int itemCount = size();
            for (int i = 0; i < itemCount; i++) {
                if (!Objects.equals(get(i), other.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int compareTo(PythonLikeTuple other) {
        int ownLength = delegate.size();
        int otherLength = other.size();
        int commonLength = Math.min(ownLength, otherLength);
        for (int i = 0; i < commonLength; i++) {
            Object ownItem = delegate.get(i);
            Object otherItem = other.delegate.get(i);
            if (ownItem instanceof Comparable ownComparable) {
                if (otherItem instanceof Comparable otherComparable) {
                    int result = ownComparable.compareTo(otherComparable);
                    if (result != 0) {
                        return result;
                    }
                } else {
                    throw new TypeError("Tuple %s does not support comparisons since item (%s) at index (%d) is not comparable."
                            .formatted(other, otherItem, i));
                }
            } else {
                throw new TypeError("Tuple %s does not support comparisons since item (%s) at index (%d) is not comparable."
                        .formatted(this, ownItem, i));
            }
        }
        return ownLength - otherLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(delegate);
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
