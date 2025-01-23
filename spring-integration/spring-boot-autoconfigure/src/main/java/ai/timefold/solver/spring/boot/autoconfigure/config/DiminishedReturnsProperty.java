package ai.timefold.solver.spring.boot.autoconfigure.config;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.convert.DurationStyle;

public enum DiminishedReturnsProperty {
    ENABLED("enabled", DiminishedReturnsProperties::setEnabled,
            value -> Boolean.parseBoolean((String) value)),
    SLIDING_WINDOW_DURATION("sliding-window-duration", DiminishedReturnsProperties::setSlidingWindowDuration,
            value -> DurationStyle.detectAndParse((String) value)),
    MINIMUM_IMPROVEMENT_RATIO("minimum-improvement-ratio", DiminishedReturnsProperties::setMinimumImprovementRatio,
            value -> Double.valueOf((String) value)),;

    private final String propertyName;
    private final BiConsumer<DiminishedReturnsProperties, Object> propertyUpdater;
    private static final Set<String> PROPERTY_NAMES = Stream.of(DiminishedReturnsProperty.values())
            .map(DiminishedReturnsProperty::getPropertyName)
            .collect(Collectors.toCollection(TreeSet::new));

    <T> DiminishedReturnsProperty(String propertyName, BiConsumer<DiminishedReturnsProperties, T> propertySetter,
            Function<Object, T> propertyConvertor) {
        this.propertyName = propertyName;
        this.propertyUpdater = (properties, object) -> propertySetter.accept(properties, propertyConvertor.apply(object));
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void update(DiminishedReturnsProperties properties, Object value) {
        propertyUpdater.accept(properties, value);
    }

    public static Set<String> getValidPropertyNames() {
        return Collections.unmodifiableSet(PROPERTY_NAMES);
    }

    public static DiminishedReturnsProperty forPropertyName(String propertyName) {
        for (var property : values()) {
            if (property.getPropertyName().equals(propertyName)) {
                return property;
            }
        }
        throw new IllegalArgumentException("No property with the name (%s). Valid properties are %s."
                .formatted(propertyName, PROPERTY_NAMES));
    }
}
