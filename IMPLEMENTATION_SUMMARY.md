# Implementation Summary: ConstraintProvider Instance Support

## Overview

This implementation adds support for passing a `ConstraintProvider` instance to the solver configuration, rather than only being able to specify a class reference. This addresses issue #1383 and enables more flexible constraint configurations at runtime.

## Changes Made

### 1. Core Configuration Class: `ScoreDirectorFactoryConfig.java`

**Location:** `/workspaces/timefold-solver/core/src/main/java/ai/timefold/solver/core/config/score/director/ScoreDirectorFactoryConfig.java`

**Changes:**

- Added new field `constraintProvider` (annotated with `@XmlTransient` to prevent XML serialization)
- Added `getConstraintProvider()` getter method
- Added `setConstraintProvider()` setter method
- Added `withConstraintProvider()` fluent API method
- Updated `inherit()` method to handle the new field
- Updated `visitReferencedClasses()` to include the instance's class when present

**Key Design Decision:** The instance is marked with `@XmlTransient` because instances cannot be serialized to XML configuration files. This maintains backward compatibility with XML-based configurations while adding programmatic configuration support.

### 2. Factory Class: `BavetConstraintStreamScoreDirectorFactory.java`

**Location:** `/workspaces/timefold-solver/core/src/main/java/ai/timefold/solver/core/impl/score/director/stream/BavetConstraintStreamScoreDirectorFactory.java`

**Changes:**

- Updated `buildScoreDirectorFactory()` to check for a provided instance first
- If an instance is provided:
  - Use it directly
  - Validate that custom properties are not also provided (as they can only be applied via reflection to class-based instantiation)
- If no instance is provided:
  - Fall back to the original class-based instantiation approach
  - Continue to support custom properties via reflection

**Key Design Decision:** Instance-based configuration takes precedence over class-based configuration, but they are mutually exclusive (validated in the factory).

### 3. Validation Class: `ScoreDirectorFactoryFactory.java`

**Location:** `/workspaces/timefold-solver/core/src/main/java/ai/timefold/solver/core/impl/score/director/ScoreDirectorFactoryFactory.java`

**Changes in `assertCorrectDirectorFactory()`:**

- Updated validation to recognize both `constraintProviderClass` and `constraintProvider` instance
- Added validation to ensure both are not provided simultaneously
- Updated error messages to mention both options
- Ensured custom properties cannot be used with instance-based configuration

**Changes in `decideMultipleScoreDirectorFactories()`:**

- Updated the condition to check for either class or instance when deciding to use constraint streams

### 4. Test Class: `ScoreDirectorFactoryFactoryTest.java`

**Location:** `/workspaces/timefold-solver/core/src/test/java/ai/timefold/solver/core/impl/score/director/ScoreDirectorFactoryFactoryTest.java`

**New Tests Added:**

1. `constraintProviderInstance()` - Verifies basic instance usage
2. `constraintProviderInstanceAndClass_throwsException()` - Verifies mutual exclusivity
3. `constraintProviderInstanceWithCustomProperties_throwsException()` - Verifies custom properties cannot be used with instances

### 5. Integration Test: `ConstraintProviderInstanceIntegrationTest.java`

**Location:** `/workspaces/timefold-solver/core/src/test/java/ai/timefold/solver/core/impl/score/director/ConstraintProviderInstanceIntegrationTest.java`

**Purpose:** Demonstrates end-to-end usage of the feature, including:

- Creating a parameterized constraint provider
- Using it with a full solver configuration
- Comparing different configurations (strict vs lenient)

### 6. Documentation: `CONSTRAINT_PROVIDER_INSTANCE_EXAMPLE.md`

**Location:** `/workspaces/timefold-solver/CONSTRAINT_PROVIDER_INSTANCE_EXAMPLE.md`

**Contents:**

- Usage examples
- Migration guide
- Important notes about serialization and custom properties
- Use cases and scenarios
- Complete working examples

## API Usage

### Simple Usage

```java
MyConstraintProvider provider = new MyConstraintProvider(config);
ScoreDirectorFactoryConfig factoryConfig = new ScoreDirectorFactoryConfig()
    .withConstraintProvider(provider);
```

### Full Solver Configuration

```java
MyConstraintProvider provider = new MyConstraintProvider(runtimeConfig);
SolverConfig solverConfig = new SolverConfig()
    .withSolutionClass(MySolution.class)
    .withEntityClasses(MyEntity.class)
    .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
        .withConstraintProvider(provider));
Solver<MySolution> solver = SolverFactory.create(solverConfig).buildSolver();
```

## Backward Compatibility

✅ **Fully backward compatible** - All existing code continues to work:

- Class-based configuration still works exactly as before
- XML serialization still works for class-based configurations
- Custom properties still work with class-based configurations
- No changes to the ConstraintProvider interface

## Validation Rules

The implementation enforces the following rules:

1. **Mutual Exclusivity:** Cannot provide both `constraintProviderClass` and `constraintProvider` instance
2. **Custom Properties:** Cannot use `constraintProviderCustomProperties` with instance-based configuration
3. **XML Serialization:** Instance-based configuration cannot be serialized to XML (by design)

## Use Cases Enabled

This feature enables several important use cases mentioned in issue #1383:

1. **Multi-tenant SaaS Applications:** Different tenants can have different constraint configurations loaded from a database
2. **Dynamic Constraint Selection:** Enable/disable constraints based on runtime conditions
3. **Parameterized Constraints:** Pass runtime parameters to constraint providers
4. **A/B Testing:** Compare different constraint configurations easily
5. **Database-driven Configuration:** Load constraint parameters and enabled/disabled status from external sources

## Testing

The implementation includes:

- ✅ Unit tests for validation logic
- ✅ Integration tests demonstrating end-to-end usage
- ✅ Tests for error conditions (mutual exclusivity, custom properties)
- ✅ No compilation errors

## Migration Path

For users currently working around this limitation (e.g., using static fields or other hacky solutions):

**Before (workaround using static fields):**

```java
public class MyConstraintProvider implements ConstraintProvider {
    private static Config config; // Not ideal!

    public static void setConfig(Config config) {
        MyConstraintProvider.config = config;
    }
}
```

**After (clean solution with instances):**

```java
public class MyConstraintProvider implements ConstraintProvider {
    private final Config config;

    public MyConstraintProvider(Config config) {
        this.config = config;
    }
}

// Usage:
Config config = loadConfig();
new ScoreDirectorFactoryConfig()
    .withConstraintProvider(new MyConstraintProvider(config))
```

## Performance Considerations

- ✅ No performance impact - instance is used directly, no additional overhead
- ✅ No breaking changes to constraint streaming implementation
- ✅ Validation happens at configuration time, not during solving

## Future Enhancements

Potential future enhancements (not included in this implementation):

- Support for serializing instances using custom serialization mechanisms
- Builder pattern for constraint provider configurations
- Integration with Spring dependency injection

## Conclusion

This implementation successfully addresses issue #1383 by providing a clean, well-tested API for using constraint provider instances while maintaining full backward compatibility with existing code and configurations.
