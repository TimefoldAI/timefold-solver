## ADDED Requirements

### Requirement: Jackson 2 序列化支持

系统 SHALL 使用 Jackson 2 (`com.fasterxml.jackson`) API 提供 Score 类型的序列化和反序列化支持，而非 Jackson 3 (`tools.jackson`)。

#### Scenario: Score 对象序列化

- **WHEN** 通过 Jackson 2 `ObjectMapper` 将 `HardSoftScore` 对象序列化为 JSON
- **THEN** 输出格式为 `{"hardScore": <long>, "softScore": <long>}`，与当前格式一致

#### Scenario: Score 对象反序列化

- **WHEN** 通过 Jackson 2 `ObjectMapper` 将 JSON 字符串反序列化为 `HardSoftScore` 对象
- **THEN** 正确还原 Score 对象的 hardScore 和 softScore 值

### Requirement: Jackson Module 自动注册

系统 SHALL 提供 `TimefoldJacksonModule`（实现 `com.fasterxml.jackson.databind.Module`）用于注册所有 Timefold 自定义序列化器和反序列化器。

#### Scenario: Module 注册

- **WHEN** 用户调用 `TimefoldJacksonModule.createModule()` 并将返回的 Module 注册到 `ObjectMapper`
- **THEN** ObjectMapper 能够正确序列化和反序列化所有 Timefold Score 类型、ConstraintRef 类型、以及常用的约束流数据类型（Break、Sequence、SequenceChain、LoadBalance）

#### Scenario: Spring Boot 自动配置下自动注册

- **WHEN** Spring Boot 应用 classpath 上同时存在 `JsonMapper` 类和 Score 类
- **THEN** `TimefoldJacksonConfiguration` 自动创建一个 `JacksonModule` Bean 并注册到 Spring 的 Jackson 自动配置中

### Requirement: Score 全类型 Jackson 2 支持

系统 SHALL 支持以下 Score 类型在 Jackson 2 下的完整序列化/反序列化 round-trip：

- `SimpleScore`
- `SimpleBigDecimalScore`
- `HardSoftScore`
- `HardSoftBigDecimalScore`
- `HardMediumSoftScore`
- `HardMediumSoftBigDecimalScore`
- `BendableScore`
- `BendableBigDecimalScore`

#### Scenario: 每种 Score 类型的 round-trip

- **WHEN** 将每种 Score 类型的实例通过 Jackson 2 序列化再反序列化
- **THEN** 反序列化后的对象 `equals` 原始对象

### Requirement: SolutionFileIO 使用 Jackson 2

系统 SHALL 通过 `JacksonSolutionFileIO` 使用 Jackson 2 `ObjectMapper` 读写规划求解的输入输出文件。

#### Scenario: 读取 Solution JSON 文件

- **WHEN** 使用 `JacksonSolutionFileIO` 读取一个包含规划解决方案的 JSON 文件
- **THEN** 正确反序列化为 `@PlanningSolution` 注解的 Java 对象

#### Scenario: 写入 Solution JSON 文件

- **WHEN** 使用 `JacksonSolutionFileIO` 将规划解决方案写入 JSON 文件
- **THEN** 输出格式正确的 JSON 文件，可被同一 `JacksonSolutionFileIO` 再次读取

### Requirement: 泛型 Score 多态序列化

系统 SHALL 支持 `Score` 接口的泛型多态序列化/反序列化，允许字段声明为 `Score` 类型而运行时确定具体实现类。

#### Scenario: 多态反序列化

- **WHEN** JSON 中包含 `@class` 属性指定具体 Score 类型（如 `ai.timefold.solver.core.api.score.HardSoftScore`）
- **THEN** Jackson 2 正确反序列化为指定的具体 Score 实现类
