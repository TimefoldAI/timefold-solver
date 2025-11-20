# ConstraintProvider Instance Support

## Overview

This feature allows you to pass a `ConstraintProvider` instance directly to the solver configuration, instead of only being able to provide a class reference. This enables more flexible constraint configurations at runtime.

## Usage

### Using ConstraintProvider Class (Original Method)

```java
SolverConfig solverConfig = new SolverConfig()
    .withSolutionClass(MySolution.class)
    .withEntityClasses(MyEntity.class)
    .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
        .withConstraintProviderClass(MyConstraintProvider.class));

SolverFactory<MySolution> solverFactory = SolverFactory.create(solverConfig);
Solver<MySolution> solver = solverFactory.buildSolver();
```

### Using ConstraintProvider Instance (New Method)

```java
// Create your constraint provider instance with custom configuration
MyConstraintProvider constraintProvider = new MyConstraintProvider(
    customParameter1,
    customParameter2,
    runtimeConfiguration
);

SolverConfig solverConfig = new SolverConfig()
    .withSolutionClass(MySolution.class)
    .withEntityClasses(MyEntity.class)
    .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
        .withConstraintProvider(constraintProvider));

SolverFactory<MySolution> solverFactory = SolverFactory.create(solverConfig);
Solver<MySolution> solver = solverFactory.buildSolver();
```

## Example: Dynamic Constraint Configuration

```java
public class ConfigurableConstraintProvider implements ConstraintProvider {

    private final Set<String> enabledConstraints;
    private final Map<String, Integer> constraintWeights;

    public ConfigurableConstraintProvider(
            Set<String> enabledConstraints,
            Map<String, Integer> constraintWeights) {
        this.enabledConstraints = enabledConstraints;
        this.constraintWeights = constraintWeights;
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        List<Constraint> constraints = new ArrayList<>();

        if (enabledConstraints.contains("capacity")) {
            constraints.add(capacityConstraint(constraintFactory));
        }

        if (enabledConstraints.contains("time-window")) {
            constraints.add(timeWindowConstraint(constraintFactory));
        }

        return constraints.toArray(new Constraint[0]);
    }

    private Constraint capacityConstraint(ConstraintFactory constraintFactory) {
        int weight = constraintWeights.getOrDefault("capacity", 1);
        return constraintFactory.forEach(Vehicle.class)
            .filter(vehicle -> vehicle.getCapacity() < vehicle.getDemand())
            .penalize(HardSoftScore.ONE_HARD.multiply(weight))
            .asConstraint("Capacity constraint");
    }

    // ... other constraints
}

// Usage in a multi-tenant SaaS application:
public Solver<MySolution> createSolverForTenant(Long tenantId) {
    TenantConfiguration config = tenantConfigRepository.findById(tenantId);

    ConfigurableConstraintProvider constraintProvider =
        new ConfigurableConstraintProvider(
            config.getEnabledConstraints(),
            config.getConstraintWeights()
        );

    SolverConfig solverConfig = new SolverConfig()
        .withSolutionClass(MySolution.class)
        .withEntityClasses(MyEntity.class)
        .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
            .withConstraintProvider(constraintProvider));

    return SolverFactory.create(solverConfig).buildSolver();
}
```

## Important Notes

### Serialization

When using a `ConstraintProvider` instance:

- The instance is **NOT** serialized when the `SolverConfig` is written to XML
- The instance is marked with `@XmlTransient` to prevent serialization
- Only the class-based configuration can be serialized to XML files

### Custom Properties

- Custom properties (`constraintProviderCustomProperties`) can **ONLY** be used with class-based configuration
- If you provide an instance, you cannot use custom properties (an exception will be thrown)
- Configure your instance directly in Java code instead of using custom properties

### Mutual Exclusivity

You **cannot** provide both a class and an instance:

```java
// This will throw an IllegalStateException:
new ScoreDirectorFactoryConfig()
    .withConstraintProviderClass(MyConstraintProvider.class)
    .withConstraintProvider(new MyConstraintProvider())  // ERROR!
```

Choose one or the other:

- Use `withConstraintProviderClass()` for simple, serializable configurations
- Use `withConstraintProvider()` for runtime-configurable, instance-based configurations

## Use Cases

This feature is particularly useful for:

1. **Multi-tenant Applications**: Different tenants can have different constraint configurations
2. **Dynamic Constraint Selection**: Enable/disable constraints based on runtime conditions
3. **Database-driven Configuration**: Load constraint parameters from a database
4. **A/B Testing**: Compare different constraint configurations
5. **Parameterized Constraints**: Pass runtime parameters to your constraint provider

## Migration from Class-based to Instance-based

If you're migrating from class-based to instance-based configuration:

**Before:**

```java
public class MyConstraintProvider implements ConstraintProvider {
    // No constructor parameters - instantiated by reflection

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        // All configuration had to be hardcoded or use static fields
    }
}
```

**After:**

```java
public class MyConstraintProvider implements ConstraintProvider {
    private final MyConfiguration config;

    public MyConstraintProvider(MyConfiguration config) {
        this.config = config;
    }

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        // Use instance configuration
        if (config.isFeatureEnabled("feature1")) {
            // ...
        }
    }
}

// Usage:
MyConfiguration config = loadConfiguration();
ConstraintProvider provider = new MyConstraintProvider(config);
solverConfig.getScoreDirectorFactoryConfig()
    .withConstraintProvider(provider);
```
