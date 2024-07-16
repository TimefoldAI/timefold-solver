package org.acme;

import java.util.function.Function;

public class MyClass {
    public static <T> T iterate(int times, T start, Function<T, T> reducer) {
        T current = start;
        for (int i = 0; i < times; i++) {
            current = reducer.apply(current);
        }
        return current;
    }
}
