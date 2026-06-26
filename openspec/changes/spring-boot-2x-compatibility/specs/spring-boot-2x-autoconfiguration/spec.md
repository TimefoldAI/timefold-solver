## ADDED Requirements

### Requirement: 自动配置与 Spring Boot 2.7 兼容

系统 SHALL 在 Spring Boot 2.7.x 环境下正确加载所有 Timefold 自动配置类，通过 `META-INF/spring.factories` 注册。

#### Scenario: 自动配置注册文件存在

- **WHEN** 应用启动时 Spring Boot 扫描 `META-INF/spring.factories`
- **THEN** 系统找到 `org.springframework.boot.autoconfigure.EnableAutoConfiguration` 键下注册的 `TimefoldSolverAutoConfiguration`、`TimefoldBenchmarkAutoConfiguration` 和 `TimefoldSolverBeanFactory`

#### Scenario: AutoConfiguration.imports 文件不影响启动

- **WHEN** 应用在 SB 2.7 中启动且 classpath 上存在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件
- **THEN** SB 2.7 忽略该文件，不产生任何错误或警告日志（该文件仅 SB 3.0+ 识别）

### Requirement: SolverConfig Bean 自动装配

系统 SHALL 自动扫描 classpath 上的 `@PlanningSolution` 和 `@PlanningEntity` 注解类，创建对应的 `SolverConfig` Bean，支持单求解器和多求解器两种模式。

#### Scenario: 单求解器、无 XML 配置

- **WHEN** classpath 上存在一个 `@PlanningSolution` 注解类、一个 `ConstraintProvider` 实现类，且未配置 timefold.solver-config-xml
- **THEN** 系统自动创建一个名为 "defaultSolverConfig" 的 `SolverConfig` Bean，其 solutionClass 和 entityClassList 被正确设置

#### Scenario: 多求解器配置

- **WHEN** timefold.solver 属性配置了多个命名求解器（如 `timefold.solver.solver1.*` 和 `timefold.solver.solver2.*`）
- **THEN** 系统为每个配置的求解器创建一个 `SolverManager` Bean，名称对应配置的 key

#### Scenario: 无 @PlanningSolution 类时不报错

- **WHEN** classpath 上没有任何 `@PlanningSolution` 或 `@PlanningEntity` 注解类
- **THEN** 系统输出 WARN 日志并跳过 Timefold 加载，应用正常启动

### Requirement: SolverFactory Bean 自动装配

系统 SHALL 在存在 SolverConfig Bean 时自动创建 `SolverFactory` Bean。

#### Scenario: SolverFactory 正常创建

- **WHEN** 存在一个有效的 SolverConfig Bean
- **THEN** 系统自动创建并提供 `SolverFactory` Bean（Lazy 初始化）

#### Scenario: 多求解器场景下不创建 SolverFactory

- **WHEN** 配置了多个命名求解器
- **THEN** 尝试注入 `SolverFactory` Bean 时抛出 `BeanCreationException`，提示应改为注入具体名称的 `SolverManager`

### Requirement: SolverManager Bean 自动装配

系统 SHALL 在存在 SolverFactory Bean 时自动创建 `SolverManager` Bean。

#### Scenario: SolverManager 正常创建

- **WHEN** 存在一个有效的 SolverFactory Bean
- **THEN** 系统根据 `timefold.solver-manager.parallel-solver-count` 配置创建 `SolverManager` Bean

### Requirement: SolutionManager Bean 自动装配

系统 SHALL 在存在 SolverFactory Bean 时自动创建 `SolutionManager` Bean。

#### Scenario: SolutionManager 正常创建

- **WHEN** 存在一个有效的 SolverFactory Bean
- **THEN** 系统自动创建 `SolutionManager` Bean

### Requirement: ConstraintVerifier Bean 自动装配

系统 SHALL 在存在 ConstraintProvider 类且可用 `ConstraintVerifier` 类时，自动创建 `ConstraintVerifier` Bean。

#### Scenario: ConstraintVerifier 正常创建

- **WHEN** SolverConfig 配置了 ConstraintProvider 类，且 classpath 上有 `ConstraintVerifier` 类
- **THEN** 系统自动创建对应的 `ConstraintVerifier` Bean

#### Scenario: 无 ConstraintProvider 时返回占位 Bean

- **WHEN** SolverConfig 未配置 ConstraintProvider 类
- **THEN** 系统返回一个抛出 `UnsupportedOperationException` 的占位 `ConstraintVerifier` Bean，不阻止应用启动

### Requirement: 基准测试自动配置

系统 SHALL 在 classpath 上存在 `PlannerBenchmarkFactory` 类时，自动创建 `PlannerBenchmarkConfig` 和 `PlannerBenchmarkFactory` Bean。

#### Scenario: Benchmark 正常创建

- **WHEN** classpath 上存在 `PlannerBenchmarkFactory` 类，且存在有效的 SolverConfig Bean
- **THEN** 系统自动创建 `PlannerBenchmarkConfig` 和 `PlannerBenchmarkFactory` Bean

#### Scenario: 多求解器场景下不允许 Benchmark

- **WHEN** 配置了多个命名求解器
- **THEN** 尝试创建 Benchmark Bean 时抛出 `IllegalStateException`

### Requirement: @EntityScan 注解识别

系统 SHALL 支持 `@EntityScan` 注解（位于 `org.springframework.boot.autoconfigure.domain.EntityScan`），用于指定 Timefold 领域类的扫描包。

#### Scenario: @EntityScan 指定额外扫描包

- **WHEN** `@SpringBootApplication` 类上使用了 `@EntityScan(basePackages = "com.example.domain")`
- **THEN** `IncludeAbstractClassesEntityScanner` 同时扫描默认包和 `com.example.domain` 包中的 `@PlanningSolution` 和 `@PlanningEntity` 注解类

### Requirement: Spring Boot 属性绑定

系统 SHALL 支持 `timefold.*` 前缀的属性配置，通过 `@EnableConfigurationProperties(TimefoldProperties.class)` 绑定。

#### Scenario: 属性正确绑定

- **WHEN** application.properties 中配置了 `timefold.solver-config-xml=classpath:solverConfig.xml`
- **THEN** `TimefoldProperties` 中 `solverConfigXml` 字段的值为 `classpath:solverConfig.xml`

### Requirement: 终止条件属性配置

系统 SHALL 支持通过 application.properties 配置求解终止条件，覆盖 XML 中的配置。

#### Scenario: 配置 spent-limit

- **WHEN** 配置了 `timefold.solver.termination.spent-limit=30s`
- **THEN** SolverConfig 的 TerminationConfig 中 spentLimit 被设置为 30 秒
