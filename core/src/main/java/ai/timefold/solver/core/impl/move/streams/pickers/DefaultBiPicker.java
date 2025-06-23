package ai.timefold.solver.core.impl.move.streams.pickers;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.impl.move.streams.maybeapi.stream.pickers.BiPicker;

import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings({ "unchecked", "rawtypes" })
public final class DefaultBiPicker<A, B>
        extends AbstractPicker<B>
        implements BiPicker<A, B> {

    private static final DefaultBiPicker NONE = new DefaultBiPicker(new Function[0], new PickerType[0], new Function[0]);

    private final Function<A, ?>[] leftMappings;

    public <Property_> DefaultBiPicker(Function<A, Property_> leftMapping, PickerType pickerType,
            Function<B, Property_> rightMapping) {
        super(rightMapping, pickerType);
        this.leftMappings = new Function[] { leftMapping };
    }

    private <Property_> DefaultBiPicker(Function<A, Property_>[] leftMappings, PickerType[] pickerTypes,
            Function<B, Property_>[] rightMappings) {
        super(rightMappings, pickerTypes);
        this.leftMappings = leftMappings;
    }

    public static <A, B> DefaultBiPicker<A, B> merge(List<DefaultBiPicker<A, B>> pickerList) {
        if (pickerList.size() == 1) {
            return pickerList.get(0);
        }
        return pickerList.stream().reduce(NONE, DefaultBiPicker::and);
    }

    @Override
    public DefaultBiPicker<A, B> and(BiPicker<A, B> otherPicker) {
        var castPicker = (DefaultBiPicker<A, B>) otherPicker;
        var pickerCount = getJoinerCount();
        var castPickerCount = castPicker.getJoinerCount();
        var newPickerCount = pickerCount + castPickerCount;
        var newPickerTypes = Arrays.copyOf(this.pickerTypes, newPickerCount);
        var newLeftMappings = Arrays.copyOf(this.leftMappings, newPickerCount);
        var newRightMappings = Arrays.copyOf(this.rightMappings, newPickerCount);
        for (var i = 0; i < castPickerCount; i++) {
            var newJoinerIndex = i + pickerCount;
            newPickerTypes[newJoinerIndex] = castPicker.getPickerType(i);
            newLeftMappings[newJoinerIndex] = castPicker.getLeftMapping(i);
            newRightMappings[newJoinerIndex] = castPicker.getRightMapping(i);
        }
        return new DefaultBiPicker(newLeftMappings, newPickerTypes, newRightMappings);
    }

    public Function<A, Object> getLeftMapping(int index) {
        return (Function<A, Object>) leftMappings[index];
    }

    public boolean matches(A a, B b) {
        var pickerCount = getJoinerCount();
        for (var i = 0; i < pickerCount; i++) {
            var pickerType = getPickerType(i);
            var leftMapping = getLeftMapping(i).apply(a);
            var rightMapping = getRightMapping(i).apply(b);
            if (!pickerType.matches(leftMapping, rightMapping)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DefaultBiPicker<?, ?> other
                && Arrays.equals(pickerTypes, other.pickerTypes)
                && Arrays.equals(leftMappings, other.leftMappings)
                && Arrays.equals(rightMappings, other.rightMappings);
    }

    @Override
    public int hashCode() {
        var hashCode = 31;
        hashCode = hashCode * 31 + Arrays.hashCode(pickerTypes);
        hashCode = hashCode * 31 + Arrays.hashCode(leftMappings);
        hashCode = hashCode * 31 + Arrays.hashCode(rightMappings);
        return hashCode;
    }

}
