package ai.timefold.solver.spring.boot.autoconfigure.config;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.convert.DurationStyle;

public enum TerminationProperty {
    SPENT_LIMIT("spent-limit", TerminationProperties::setSpentLimit,
            value -> DurationStyle.detectAndParse((String) value)),
    UNIMPROVED_SPENT_LIMIT("unimproved-spent-limit", TerminationProperties::setUnimprovedSpentLimit,
            value -> DurationStyle.detectAndParse((String) value)),
    BEST_SCORE_LIMIT("best-score-limit", TerminationProperties::setBestScoreLimit, Object::toString);

    private final String propertyName;
    private final BiConsumer<TerminationProperties, Object> propertyUpdater;
    private static final Set<String> PROPERTY_NAMES = Stream.of(TerminationProperty.values())
            .map(TerminationProperty::getPropertyName)
            .collect(Collectors.toCollection(TreeSet::new));

    <T> TerminationProperty(String propertyName, BiConsumer<TerminationProperties, T> propertySetter,
            Function<Object, T> propertyConvertor) {
        this.propertyName = propertyName;
        this.propertyUpdater = (properties, object) -> propertySetter.accept(properties, propertyConvertor.apply(object));
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void update(TerminationProperties properties, Object value) {
        propertyUpdater.accept(properties, value);
    }

    public static Set<String> getValidPropertyNames() {
        return Collections.unmodifiableSet(PROPERTY_NAMES);
    }

    public static TerminationProperty forPropertyName(String propertyName) {
        for (var property : values()) {
            if (property.getPropertyName().equals(propertyName)) {
                return property;
            }
        }
        throw new IllegalArgumentException("No property with the name (%s). Valid properties are %s."
                .formatted(propertyName, PROPERTY_NAMES));
    }
}
