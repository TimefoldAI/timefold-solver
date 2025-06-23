package ai.timefold.solver.core.impl.move.streams.generic.common.pickers;

import java.util.Objects;
import java.util.function.Function;

import org.jspecify.annotations.NullMarked;

@NullMarked
@SuppressWarnings({ "unchecked" })
public sealed abstract class AbstractPicker<Right_>
        permits DefaultBiPicker {

    protected final Function<Right_, Object>[] rightMappings;
    protected final PickerType[] pickerTypes;

    protected <Property_> AbstractPicker(Function<Right_, Property_> rightMapping, PickerType pickerType) {
        this(new Function[] { rightMapping }, new PickerType[] { pickerType });
    }

    protected <Property_> AbstractPicker(Function<Right_, Property_>[] rightMappings, PickerType[] pickerTypes) {
        this.rightMappings = (Function<Right_, Object>[]) Objects.requireNonNull(rightMappings);
        this.pickerTypes = Objects.requireNonNull(pickerTypes);
    }

    public final Function<Right_, Object> getRightMapping(int index) {
        return rightMappings[index];
    }

    public final int getJoinerCount() {
        return pickerTypes.length;
    }

    public final PickerType getPickerType(int index) {
        return pickerTypes[index];
    }

}
