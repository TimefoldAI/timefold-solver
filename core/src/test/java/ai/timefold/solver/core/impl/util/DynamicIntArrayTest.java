package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DynamicIntArrayTest {

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Default constructor initializes with max size Integer.MAX_VALUE")
        void defaultConstructor() {
            var array = new DynamicIntArray();

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(array::getFirstIndex)
                    .withMessage("Array is empty");

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(array::getLastIndex)
                    .withMessage("Array is empty");

            assertThat(array.containsIndex(0)).isFalse();
            assertThat(array.get(0)).isZero();
        }

        @Test
        @DisplayName("Constructor with maxSize initializes correctly")
        void constructorWithMaxSize() {
            var array = new DynamicIntArray(100);

            assertThatExceptionOfType(IllegalStateException.class)
                    .isThrownBy(array::getFirstIndex);

            // Test bound checking with maxSize
            assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
                    .isThrownBy(() -> array.set(100, 5));
        }
    }

    @Nested
    @DisplayName("Set method tests")
    class SetMethodTests {

        @Test
        @DisplayName("Set first element initializes the array")
        void setFirstElement() {
            var array = new DynamicIntArray();
            array.set(10, 42);

            assertThat(array.get(10)).isEqualTo(42);
            assertThat(array.getFirstIndex()).isEqualTo(10);
            assertThat(array.getLastIndex()).isEqualTo(10);
            assertThat(array.containsIndex(10)).isTrue();
            assertThat(array.containsIndex(9)).isFalse();
            assertThat(array.containsIndex(11)).isFalse();
        }

        @Test
        @DisplayName("Set lower index than first index reallocates the array")
        void setLowerIndex() {
            var array = new DynamicIntArray();
            array.set(10, 42);
            array.set(5, 24);

            assertThat(array.get(5)).isEqualTo(24);
            assertThat(array.get(10)).isEqualTo(42);
            assertThat(array.getFirstIndex()).isEqualTo(5);
            assertThat(array.getLastIndex()).isEqualTo(10);
            assertThat(array.containsIndex(5)).isTrue();
            assertThat(array.containsIndex(10)).isTrue();
        }

        @Test
        @DisplayName("Set higher index than last index expands the array")
        void setHigherIndex() {
            var array = new DynamicIntArray();
            array.set(5, 24);
            array.set(10, 42);

            assertThat(array.get(5)).isEqualTo(24);
            assertThat(array.get(10)).isEqualTo(42);
            assertThat(array.getFirstIndex()).isEqualTo(5);
            assertThat(array.getLastIndex()).isEqualTo(10);
            assertThat(array.containsIndex(5)).isTrue();
            assertThat(array.containsIndex(10)).isTrue();
        }

        @Test
        @DisplayName("Set existing index updates the value")
        void setExistingIndex() {
            var array = new DynamicIntArray();
            array.set(5, 24);
            array.set(10, 42);
            array.set(7, 99);
            array.set(7, 100); // Update existing value

            assertThat(array.get(7)).isEqualTo(100);
            assertThat(array.getFirstIndex()).isEqualTo(5);
            assertThat(array.getLastIndex()).isEqualTo(10);
        }

        @ParameterizedTest
        @ValueSource(ints = { -1, -5, -100 })
        @DisplayName("Set negative index throws ArrayIndexOutOfBoundsException")
        void setNegativeIndex(int index) {
            var array = new DynamicIntArray();

            assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
                    .isThrownBy(() -> array.set(index, 42));
        }

        @Test
        @DisplayName("Set index greater than or equal to maxSize throws ArrayIndexOutOfBoundsException")
        void setIndexGreaterThanMaxSize() {
            var array = new DynamicIntArray(50);

            assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
                    .isThrownBy(() -> array.set(50, 42));

            assertThatExceptionOfType(ArrayIndexOutOfBoundsException.class)
                    .isThrownBy(() -> array.set(100, 42));
        }
    }

    @Nested
    @DisplayName("Get method tests")
    class GetMethodTests {

        @Test
        @DisplayName("Get returns 0 for empty array")
        void getFromEmptyArray() {
            var array = new DynamicIntArray();

            assertThat(array.get(5)).isZero();
        }

        @Test
        @DisplayName("Get returns 0 for index lower than first index")
        void getIndexLowerThanFirstIndex() {
            var array = new DynamicIntArray();
            array.set(10, 42);

            assertThat(array.get(5)).isZero();
        }

        @Test
        @DisplayName("Get returns 0 for index higher than last index")
        void getIndexHigherThanLastIndex() {
            var array = new DynamicIntArray();
            array.set(10, 42);

            assertThat(array.get(15)).isZero();
        }

        @Test
        @DisplayName("Get returns correct value for existing index")
        void getExistingIndex() {
            var array = new DynamicIntArray();
            array.set(5, 24);
            array.set(10, 42);

            assertThat(array.get(5)).isEqualTo(24);
            assertThat(array.get(10)).isEqualTo(42);
        }
    }

    @Nested
    @DisplayName("ContainsIndex method tests")
    class ContainsIndexMethodTests {

        @Test
        @DisplayName("ContainsIndex returns false for empty array")
        void containsIndexEmptyArray() {
            var array = new DynamicIntArray();

            assertThat(array.containsIndex(0)).isFalse();
            assertThat(array.containsIndex(5)).isFalse();
        }

        @Test
        @DisplayName("ContainsIndex returns true for indices within range")
        void containsIndexWithinRange() {
            var array = new DynamicIntArray();
            array.set(5, 24);
            array.set(10, 42);

            assertThat(array.containsIndex(5)).isTrue();
            assertThat(array.containsIndex(7)).isTrue();
            assertThat(array.containsIndex(10)).isTrue();
        }

        @Test
        @DisplayName("ContainsIndex returns false for indices outside range")
        void containsIndexOutsideRange() {
            var array = new DynamicIntArray();
            array.set(5, 24);
            array.set(10, 42);

            assertThat(array.containsIndex(4)).isFalse();
            assertThat(array.containsIndex(11)).isFalse();
        }
    }

    @Nested
    @DisplayName("Complex scenario tests")
    class ComplexScenarioTests {

        @Test
        @DisplayName("Test multiple operations in sequence")
        void testMultipleOperations() {
            var array = new DynamicIntArray();

            // Initial setup
            array.set(10, 42);
            assertThat(array.get(10)).isEqualTo(42);
            assertThat(array.getFirstIndex()).isEqualTo(10);
            assertThat(array.getLastIndex()).isEqualTo(10);

            // Expand below
            array.set(5, 24);
            assertThat(array.get(5)).isEqualTo(24);
            assertThat(array.get(10)).isEqualTo(42);
            assertThat(array.getFirstIndex()).isEqualTo(5);
            assertThat(array.getLastIndex()).isEqualTo(10);

            // Expand above
            array.set(15, 99);
            assertThat(array.get(5)).isEqualTo(24);
            assertThat(array.get(10)).isEqualTo(42);
            assertThat(array.get(15)).isEqualTo(99);
            assertThat(array.getFirstIndex()).isEqualTo(5);
            assertThat(array.getLastIndex()).isEqualTo(15);

            // Update existing
            array.set(10, 100);
            assertThat(array.get(10)).isEqualTo(100);

            // Verify indices not explicitly set
            assertThat(array.get(7)).isZero();
            assertThat(array.get(12)).isZero();

            // Verify contains index
            assertThat(array.containsIndex(5)).isTrue();
            assertThat(array.containsIndex(7)).isTrue();
            assertThat(array.containsIndex(15)).isTrue();
            assertThat(array.containsIndex(4)).isFalse();
            assertThat(array.containsIndex(16)).isFalse();
        }

        @Test
        @DisplayName("Test with sparse indices")
        void testWithSparseIndices() {
            var array = new DynamicIntArray();

            array.set(100, 1);
            array.set(1000, 2);
            array.set(10, 3);

            assertThat(array.getFirstIndex()).isEqualTo(10);
            assertThat(array.getLastIndex()).isEqualTo(1000);
            assertThat(array.get(10)).isEqualTo(3);
            assertThat(array.get(100)).isEqualTo(1);
            assertThat(array.get(1000)).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Clear method tests")
    class ClearMethodTests {

        @Test
        @DisplayName("Clear on empty array does nothing")
        void clearEmptyArray() {
            var array = new DynamicIntArray();

            // Should not throw an exception
            array.clear();
        }

        @Test
        @DisplayName("Clear resets all values to 0 but preserves array structure")
        void clearResetsValues() {
            var array = new DynamicIntArray();
            array.set(5, 24);
            array.set(10, 42);

            array.clear();

            // Values should be reset to 0
            assertThat(array.get(5)).isZero();
            assertThat(array.get(10)).isZero();

            // Array structure should be preserved
            assertThat(array.containsIndex(5)).isFalse();
            assertThat(array.containsIndex(10)).isFalse();
        }

        @Test
        @DisplayName("Clear and then set new values")
        void clearAndSetNewValues() {
            var array = new DynamicIntArray();
            array.set(5, 24);
            array.set(10, 42);

            array.clear();

            // Set new values
            array.set(7, 99);

            // New values should be set correctly
            assertThat(array.get(7)).isEqualTo(99);

            // Old indices should still be in the array but with value 0
            assertThat(array.get(5)).isZero();
            assertThat(array.get(10)).isZero();

            // Array structure should be updated
            assertThat(array.getFirstIndex()).isEqualTo(7);
            assertThat(array.getLastIndex()).isEqualTo(7);
        }

    }

}
