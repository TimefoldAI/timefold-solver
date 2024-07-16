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
import ai.timefold.jpyinterpreter.PythonTernaryOperator;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonSlice;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.errors.lookup.IndexError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningCloneable;

public class PythonLikeList<T> extends AbstractPythonLikeObject implements List<T>,
        PlanningCloneable<PythonLikeList<T>>,
        RandomAccess {
    final List delegate;
    private int remainderToAdd;

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonLikeList::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Constructor
        BuiltinTypes.LIST_TYPE.setConstructor((positionalArguments, namedArguments, callerInstance) -> {
            if (positionalArguments.size() == 0) {
                return new PythonLikeList();
            } else if (positionalArguments.size() == 1) {
                PythonLikeList out = new PythonLikeList();
                out.extend(positionalArguments.get(0));
                return out;
            } else {
                throw new ValueError("list expects 0 or 1 arguments, got " + positionalArguments.size());
            }
        });

        // Unary methods
        BuiltinTypes.LIST_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH, PythonLikeList.class.getMethod("length"));
        BuiltinTypes.LIST_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR, PythonLikeList.class.getMethod("getIterator"));
        BuiltinTypes.LIST_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                PythonLikeList.class.getMethod("getRepresentation"));

        // Binary methods
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.ADD,
                PythonLikeList.class.getMethod("concatToNew", PythonLikeList.class));
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.INPLACE_ADD,
                PythonLikeList.class.getMethod("concatToSelf", PythonLikeList.class));
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.MULTIPLY,
                PythonLikeList.class.getMethod("multiplyToNew", PythonInteger.class));
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.INPLACE_MULTIPLY,
                PythonLikeList.class.getMethod("multiplyToSelf", PythonInteger.class));
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonLikeList.class.getMethod("getItem", PythonInteger.class));
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.GET_ITEM,
                PythonLikeList.class.getMethod("getSlice", PythonSlice.class));
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.DELETE_ITEM,
                PythonLikeList.class.getMethod("deleteItem", PythonInteger.class));
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.DELETE_ITEM,
                PythonLikeList.class.getMethod("deleteSlice", PythonSlice.class));
        BuiltinTypes.LIST_TYPE.addBinaryMethod(PythonBinaryOperator.CONTAINS,
                PythonLikeList.class.getMethod("containsItem", PythonLikeObject.class));

        // Ternary methods
        BuiltinTypes.LIST_TYPE.addTernaryMethod(PythonTernaryOperator.SET_ITEM,
                PythonLikeList.class.getMethod("setItem", PythonInteger.class, PythonLikeObject.class));
        BuiltinTypes.LIST_TYPE.addTernaryMethod(PythonTernaryOperator.SET_ITEM,
                PythonLikeList.class.getMethod("setSlice", PythonSlice.class, PythonLikeObject.class));

        // Other
        BuiltinTypes.LIST_TYPE.addMethod("append", PythonLikeList.class.getMethod("append", PythonLikeObject.class));
        BuiltinTypes.LIST_TYPE.addMethod("extend", PythonLikeList.class.getMethod("extend", PythonLikeObject.class));
        BuiltinTypes.LIST_TYPE.addMethod("insert",
                PythonLikeList.class.getMethod("insert", PythonInteger.class, PythonLikeObject.class));
        BuiltinTypes.LIST_TYPE.addMethod("remove", PythonLikeList.class.getMethod("remove", PythonLikeObject.class));
        BuiltinTypes.LIST_TYPE.addMethod("clear", PythonLikeList.class.getMethod("clearList"));
        BuiltinTypes.LIST_TYPE.addMethod("copy", PythonLikeList.class.getMethod("copy"));
        BuiltinTypes.LIST_TYPE.addMethod("count", PythonLikeList.class.getMethod("count", PythonLikeObject.class));
        BuiltinTypes.LIST_TYPE.addMethod("index", PythonLikeList.class.getMethod("index", PythonLikeObject.class));
        BuiltinTypes.LIST_TYPE.addMethod("index",
                PythonLikeList.class.getMethod("index", PythonLikeObject.class, PythonInteger.class));
        BuiltinTypes.LIST_TYPE.addMethod("index",
                PythonLikeList.class.getMethod("index", PythonLikeObject.class, PythonInteger.class, PythonInteger.class));
        BuiltinTypes.LIST_TYPE.addMethod("pop", PythonLikeList.class.getMethod("pop"));
        BuiltinTypes.LIST_TYPE.addMethod("pop", PythonLikeList.class.getMethod("pop", PythonInteger.class));
        BuiltinTypes.LIST_TYPE.addMethod("reverse", PythonLikeList.class.getMethod("reverse"));
        BuiltinTypes.LIST_TYPE.addMethod("sort", PythonLikeList.class.getMethod("sort"));

        return BuiltinTypes.LIST_TYPE;
    }

    public PythonLikeList() {
        super(BuiltinTypes.LIST_TYPE);
        delegate = new ArrayList<>();
        remainderToAdd = 0;
    }

    public PythonLikeList(int size) {
        super(BuiltinTypes.LIST_TYPE);
        delegate = new ArrayList<>(size);
        remainderToAdd = size;
        for (int i = 0; i < size; i++) {
            delegate.add(null);
        }
    }

    public PythonLikeList(List delegate) {
        super(BuiltinTypes.LIST_TYPE);
        this.delegate = delegate;
        remainderToAdd = 0;
    }

    @Override
    public PythonLikeList<T> createNewInstance() {
        return new PythonLikeList<>();
    }

    // Required for bytecode generation
    @SuppressWarnings("unused")
    public void reverseAdd(PythonLikeObject object) {
        delegate.set(remainderToAdd - 1, object);
        remainderToAdd--;
    }

    public DelegatePythonIterator getIterator() {
        return new DelegatePythonIterator(delegate.iterator());
    }

    public PythonLikeList copy() {
        PythonLikeList copy = new PythonLikeList();
        copy.addAll(delegate);
        return copy;
    }

    public PythonLikeList concatToNew(PythonLikeList other) {
        PythonLikeList result = new PythonLikeList();
        result.addAll(delegate);
        result.addAll(other);
        return result;
    }

    public PythonLikeList concatToSelf(PythonLikeList other) {
        this.addAll(other);
        return this;
    }

    public PythonLikeList multiplyToNew(PythonInteger times) {
        if (times.value.compareTo(BigInteger.ZERO) <= 0) {
            return new PythonLikeList();
        }

        PythonLikeList result = new PythonLikeList();
        int timesAsInt = times.value.intValueExact();

        for (int i = 0; i < timesAsInt; i++) {
            result.addAll(delegate);
        }

        return result;
    }

    public PythonLikeList multiplyToSelf(PythonInteger times) {
        if (times.value.compareTo(BigInteger.ZERO) <= 0) {
            delegate.clear();
            return this;
        }
        List<PythonLikeObject> copy = new ArrayList<>(delegate);
        int timesAsInt = times.value.intValueExact() - 1;

        for (int i = 0; i < timesAsInt; i++) {
            delegate.addAll(copy);
        }

        return this;
    }

    public PythonInteger length() {
        return PythonInteger.valueOf(delegate.size());
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

    public PythonLikeList getSlice(PythonSlice slice) {
        int length = delegate.size();

        PythonLikeList out = new PythonLikeList();

        slice.iterate(length, (i, processed) -> {
            out.add(delegate.get(i));
        });

        return out;
    }

    public PythonLikeObject setItem(PythonInteger index, PythonLikeObject value) {
        int indexAsInt = index.value.intValueExact();

        if (indexAsInt < 0) {
            indexAsInt = delegate.size() + index.value.intValueExact();
        }

        if (indexAsInt < 0 || indexAsInt >= delegate.size()) {
            throw new IndexError("list index out of range");
        }

        delegate.set(indexAsInt, value);
        return PythonNone.INSTANCE;
    }

    public PythonLikeObject setSlice(PythonSlice slice, PythonLikeObject iterable) {
        int length = delegate.size();
        int start = slice.getStartIndex(length);
        int stop = slice.getStopIndex(length);
        int step = slice.getStrideLength();
        Iterator<PythonLikeObject> iterator = (Iterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(iterable);

        if (step == 1) {
            delegate.subList(start, stop).clear();
            int offset = 0;
            while (iterator.hasNext()) {
                PythonLikeObject item = iterator.next();
                delegate.add(start + offset, item);
                offset++;
            }
        } else {
            List<PythonLikeObject> temp = new ArrayList<>();
            iterator.forEachRemaining(temp::add);
            if (temp.size() != slice.getSliceSize(length)) {
                throw new ValueError("attempt to assign sequence of size " + temp.size() + " to extended slice of size "
                        + slice.getSliceSize(length));
            }

            slice.iterate(length, (i, processed) -> {
                delegate.set(i, temp.get(processed));
            });
        }

        return PythonNone.INSTANCE;
    }

    public PythonNone deleteItem(PythonInteger index) {
        if (index.value.compareTo(BigInteger.ZERO) < 0) {
            delegate.remove(delegate.size() + index.value.intValueExact());
        } else {
            delegate.remove(index.value.intValueExact());
        }
        return PythonNone.INSTANCE;
    }

    public PythonNone deleteSlice(PythonSlice slice) {
        int length = delegate.size();
        int start = slice.getStartIndex(length);
        int stop = slice.getStopIndex(length);
        int step = slice.getStrideLength();

        if (step == 1) {
            delegate.subList(start, stop).clear();
        } else {
            if (step > 0) {
                // need to account for removed items because we are moving up the list,
                // (as removing items shift elements down)
                slice.iterate(length, (i, processed) -> {
                    delegate.remove(i - processed);
                });
            } else {
                // Since we are moving down the list (starting at the higher value),
                // the elements being removed stay in the same place, so we do not need
                // to account for processed elements
                slice.iterate(length, (i, processed) -> {
                    delegate.remove(i);
                });
            }
        }

        return PythonNone.INSTANCE;
    }

    public PythonNone remove(PythonLikeObject item) {
        if (!delegate.remove(item)) {
            throw new ValueError("list.remove(x): x not in list");
        }
        return PythonNone.INSTANCE;
    }

    public PythonNone insert(PythonInteger index, PythonLikeObject item) {
        int indexAsInt = PythonSlice.asIntIndexForLength(index, delegate.size());

        if (indexAsInt < 0) {
            indexAsInt = 0;
        }

        if (indexAsInt > delegate.size()) {
            indexAsInt = delegate.size();
        }

        delegate.add(indexAsInt, item);
        return PythonNone.INSTANCE;
    }

    public PythonLikeObject pop() {
        if (delegate.isEmpty()) {
            throw new IndexError("pop from empty list");
        }
        return (PythonLikeObject) delegate.remove(delegate.size() - 1);
    }

    public PythonLikeObject pop(PythonInteger index) {
        if (delegate.isEmpty()) {
            throw new IndexError("pop from empty list");
        }

        int indexAsInt = index.value.intValueExact();
        if (indexAsInt < 0) {
            indexAsInt = delegate.size() + indexAsInt;
        }

        if (indexAsInt >= delegate.size() || indexAsInt < 0) {
            throw new IndexError("pop index out of range");
        }

        return (PythonLikeObject) delegate.remove(indexAsInt);
    }

    public PythonBoolean containsItem(PythonLikeObject item) {
        return PythonBoolean.valueOf(delegate.contains(item));
    }

    public PythonInteger count(PythonLikeObject search) {
        long count = 0;
        for (Object x : delegate) {
            if (Objects.equals(search, x)) {
                count++;
            }
        }
        return new PythonInteger(count);
    }

    public PythonNone append(PythonLikeObject item) {
        delegate.add(item);
        return PythonNone.INSTANCE;
    }

    public PythonNone extend(PythonLikeObject item) {
        if (item instanceof Collection) {
            delegate.addAll((List) item);
        } else {
            Iterator iterator = (Iterator) UnaryDunderBuiltin.ITERATOR.invoke(item);
            iterator.forEachRemaining(this::add);
        }
        return PythonNone.INSTANCE;
    }

    public PythonNone reverse() {
        Collections.reverse(delegate);
        return PythonNone.INSTANCE;
    }

    public PythonNone sort() {
        Collections.sort(delegate);
        return PythonNone.INSTANCE;
    }

    public PythonNone clearList() {
        delegate.clear();
        return PythonNone.INSTANCE;
    }

    public PythonString getRepresentation() {
        return PythonString.valueOf(toString());
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

    public Object[] toArray(Object[] ts) {
        return delegate.toArray(ts);
    }

    @Override
    public boolean add(Object pythonLikeObject) {
        return delegate.add(pythonLikeObject);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection collection) {
        return delegate.containsAll(collection);
    }

    @Override
    public boolean addAll(Collection collection) {
        return delegate.addAll(collection);
    }

    @Override
    public boolean addAll(int i, Collection collection) {
        return delegate.addAll(i, collection);
    }

    @Override
    public boolean removeAll(Collection collection) {
        return delegate.removeAll(collection);
    }

    @Override
    public boolean retainAll(Collection collection) {
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
    public Object set(int i, Object pythonLikeObject) {
        return delegate.set(i, pythonLikeObject);
    }

    @Override
    public void add(int i, Object pythonLikeObject) {
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
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append('[');
        for (int i = 0; i < delegate.size() - 1; i++) {
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke((PythonLikeObject) delegate.get(i)));
            out.append(", ");
        }
        if (!delegate.isEmpty()) {
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke((PythonLikeObject) delegate.get(delegate.size() - 1)));
        }
        out.append(']');

        return out.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof List) {
            List other = (List) o;
            if (other.size() != delegate.size()) {
                return false;
            }
            int itemCount = delegate.size();
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
    public int hashCode() {
        return Objects.hash(delegate);
    }

    public List getDelegate() {
        return delegate;
    }
}
