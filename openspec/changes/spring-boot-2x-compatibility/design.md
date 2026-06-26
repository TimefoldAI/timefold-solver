## Context

Timefold Solver 当前在 `main` 分支上适配 Spring Boot 4.1.0（Spring Framework 7.x），使用 Java 21 编译，依赖 Jackson 3 (`tools.jackson`)、AOT 编译支持（Spring 6+ 引入的 `BeanFactoryInitializationAot*` API）以及 `spring-boot-persistence` 模块（SB 3.0+ 引入）。本次改造需要在独立分支 `caj/spring-boot-2x` 上将整个项目降级至 Spring Boot 2.7.18 兼容，且后续需频繁 rebase main 以保持与上游同步。

### 当前依赖关系图

```
timefold-solver-spring-integration/
├── spring-integration/pom.xml
│   ├── imports tools.jackson:jackson-bom (Jackson 3)      ← 需替换
│   └── imports spring-boot-dependencies (SB 4.1.0)        ← 需降级
├── spring-boot-starter/
│   ├── depends spring-boot-starter                        ← 版本由 BOM 管理
│   ├── depends spring-boot-persistence  ⚠️ SB 2.7 不存在   ← 需移除
│   └── depends timefold-solver-jackson (Jackson 3)        ← 需降级后适配
├── spring-boot-autoconfigure/
│   ├── depends spring-boot-persistence  ⚠️ SB 2.7 不存在   ← 需移除
│   ├── imports EntityScan/EntityScanner/EntityScanPackages
│   │   from org.springframework.boot.persistence.autoconfigure  ← 包路径迁移
│   ├── implements BeanFactoryInitializationAotProcessor   ← 需移除
│   ├── contains TimefoldSolverAotContribution             ← 需删除
│   ├── contains TimefoldSolverAotFactory                  ← 需重构
│   └── uses NativeDetector (Spring 5.3 中存在 ✓)          ← 保留但简化
└── spring-boot-integration-test/
    └── native profile uses process-aot goal               ← 需移除

timefold-solver-persistence/jackson/
├── 依赖 tools.jackson (Jackson 3)                          ← 全模块降级
├── JacksonModule 构建 API (Jackson 3 专属)                 ← 需适配
└── 60+ 文件使用 tools.jackson.* import                    ← 批量替换
```

### SB 2.7.18 关键依赖版本

| 依赖 | SB 4.1.0 版本 | SB 2.7.18 版本 |
|------|-------------|--------------|
| Spring Framework | 7.x | 5.3.31 |
| Jackson BOM | (不受 SB 管理) | 2.13.5 |
| JUnit Jupiter | 6.1.0 (自定义) | 5.8.2 |
| Hibernate | (受 SB 管理) | 5.6.15.Final |
| Micrometer | (受 SB 管理) | 1.9.17 |
| Logback | 1.5.34 (自定义) | 1.2.12 |
| SLF4J | (受 SB 管理) | 1.7.36 |
| SnakeYAML | (受 SB 管理) | 1.30 |
| Jakarta XML Bind | (自定义版本) | 2.3.3 (SB BOM) |
| Jakarta Persistence | (自定义版本) | 2.2.3 (SB BOM) |
| Jakarta Validation | (自定义版本) | 2.0.2 (SB BOM) |
| Jakarta CDI | 4.1.0 (自定义) | (不在 SB 2.7 BOM 中) |

## Goals / Non-Goals

**Goals:**

- 整个项目（core、persistence、spring-integration、service、tools）在 Java 17 + SB 2.7.18 下成功编译
- spring-integration 和 spring-boot-integration-test 的所有测试通过
- Jackson 序列化/反序列化完全兼容 Jackson 2.13.x API
- 自动配置在 SB 2.7 下的 Bean 装配行为与当前 SB 4.x 一致（多求解器、约束校验器等场景）
- Quarkus 集成模块保持原有版本不变
- 代码结构便于后续 rebase main 时最小化冲突

**Non-Goals:**

- 不支持 SB 2.7 的 GraalVM Native Image（配置保留但不保证可用）
- 不提供 Jackson 3 和 Jackson 2 双版本共存（仅保留 Jackson 2）
- 不修改 core 模块的核心引擎代码
- 不修改 service 模块（基于 Quarkus/CDI）
- 不追求与 SB 3.x 同时兼容（此分支仅针对 SB 2.7.x）

## Decisions

### 决策 1: Jackson 3 → Jackson 2 完整降级

**选择**: 将 `persistence/jackson/` 整个模块从 `tools.jackson` (Jackson 3) 降级到 `com.fasterxml.jackson` (Jackson 2.13.5)

**替代方案及否决理由**:
- ❌ 双版本共存（维护两个 Jackson 模块）: 增加维护成本，且此次目标是 SB 2.7 专项适配
- ❌ 仅在 spring-integration 中做转换层: core 和 persistence 已有深度 Jackson 集成，转换层不可行

**具体 API 映射**:

| Jackson 3 (`tools.jackson`) | Jackson 2 (`com.fasterxml.jackson`) |
|------------------------------|--------------------------------------|
| `tools.jackson.databind.JacksonModule` | `com.fasterxml.jackson.databind.Module` |
| `tools.jackson.databind.json.JsonMapper` | `com.fasterxml.jackson.databind.json.JsonMapper` (同名但有细微差异) |
| `tools.jackson.core.JacksonException` | `com.fasterxml.jackson.core.JacksonException` |
| `tools.jackson.databind.ValueDeserializer` | `com.fasterxml.jackson.databind.ValueDeserializer` |
| `tools.jackson.databind.DatabindException` | `com.fasterxml.jackson.databind.DatabindException` |

**风险**: `JsonMapper` 构造方式在 Jackson 3 中有所变化（如 builder 方法名、module 注册方式），需要逐一适配。

### 决策 2: AOT 代码处理策略

**选择**: 完全删除 AOT 相关类，将 `TimefoldSolverAutoConfiguration` 重构为纯 `@Configuration` + `@Bean` 模式

**当前 AOT 代码结构**:
```
TimefoldSolverAutoConfiguration
  ├── implements BeanFactoryInitializationAotProcessor  ← 删除
  ├── processAheadOfTime() → TimefoldSolverAotContribution  ← 删除
  └── postProcessBeanDefinitionRegistry()              ← 重构为 @Bean 方法
        └── 通过注册 RootBeanDefinition 创建 SolverConfig/SolverManager Bean
```

**重构后**:
```
TimefoldSolverAutoConfiguration
  └── @Bean 方法直接创建 SolverConfig/SolverManager Bean
      通过 TimefoldSolverAotFactory → 重命名为 SolverConfigFactory
      作为普通的 @Configuration 内部工厂方法
```

**理由**: SB 2.7 中 `@Configuration` 类本身就支持 `@Bean` 方法的条件装配，不需要通过 `BeanDefinitionRegistryPostProcessor` 手动注册 Bean。简化了代码路径，同时保持了多求解器配置场景的正确性。

**保留**: `TimefoldSolverAotFactory` 重命名为 `SolverConfigFactory`，保留其从 XML 字符串反序列化 SolverConfig 的逻辑，但不再作为 AOT 工厂，而是作为普通 Spring Bean 工厂使用。

### 决策 3: spring-boot-persistence 替换方案

**选择**: 移除 `spring-boot-persistence` 依赖，将 `EntityScan`/`EntityScanner`/`EntityScanPackages` 的 import 改为 `org.springframework.boot.autoconfigure.domain.*`

**影响细节**:
- `IncludeAbstractClassesEntityScanner` 继承的 `EntityScanner` 在 SB 2.7 中位于 `org.springframework.boot.autoconfigure.domain.EntityScanner`
- API 签名完全一致，仅包路径不同
- `@EntityScan` 注解移到 `org.springframework.boot.autoconfigure.domain.EntityScan`

### 决策 4: 自动配置注册文件

**选择**: 创建 `META-INF/spring.factories`，同时保留 `AutoConfiguration.imports`（SB 2.7 会忽略后者，不会产生冲突）

```
# spring.factories (SB 2.7 使用)
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
ai.timefold.solver.spring.boot.autoconfigure.TimefoldSolverAutoConfiguration,\
ai.timefold.solver.spring.boot.autoconfigure.TimefoldBenchmarkAutoConfiguration,\
ai.timefold.solver.spring.boot.autoconfigure.TimefoldSolverBeanFactory
```

### 决策 5: 依赖版本管理策略

**选择**: 尽量使用 SB 2.7.18 BOM 管理的版本，仅对 Timefold 明确需要的版本进行显式覆盖

**显式保留版本覆盖的依赖**:
- `jakarta.xml.bind-api` / `jaxb-runtime`: core 深度依赖，版本由 Timefold 自己管理
- `jakarta.persistence-api`: JPA 模块使用，版本独立于 SB
- `jakarta.enterprise.cdi-api`: service 模块使用（Quarkus 生态，独立于 Spring）
- `gizmo2`: Quarkus Gizmo 字节码库，独立版本
- `ow2.asm`: 字节码操作，独立版本
- `freemarker`: 模板引擎，独立版本
- `commons-math3`: 数学库，独立版本
- `jspecify`: JSpecify 注解，独立版本
- `json-schema-validator`: JSON Schema 验证，独立版本

**交由 SB 2.7 BOM 管理的依赖**:
- Jackson 2 (com.fasterxml.jackson)
- JUnit Jupiter
- Mockito
- AssertJ
- Logback / SLF4J
- Micrometer
- SnakeYAML
- Hibernate (仅 persistence/jpa 测试中使用)

### 决策 6: Java 编译器目标版本

**选择**: `maven.compiler.release` 从 21 改为 17

**理由**: SB 2.7.18 官方支持 Java 17，且 Timefold 当前代码使用的语言特性（records、var、text blocks、Stream.toList()、String.formatted()）全部在 Java 17 中已 GA。

**需要检查的风险点**: 代码中是否使用了 Java 18+ 的 API（如 `Character.isEmoji()` 等），如有需要替换为兼容实现。

## Risks / Trade-offs

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|----------|
| Jackson 2 API 细微差异导致序列化行为变化 | 中 | 高 | 运行全部 persistence/jackson 测试 + spring-integration 测试，验证 round-trip 序列化 |
| AOT 重构后多求解器场景下 Bean 装配顺序问题 | 中 | 中 | 保留现有的多求解器测试用例（`TimefoldSolverMultipleSolverAutoConfigurationTest`），确保全部通过 |
| SB 2.7 的 Spring 5.3 `@Configuration(proxyBeanMethods=false)` 行为差异 | 低 | 低 | 两个版本均支持此配置，行为一致 |
| WireMock 版本降级可能导致测试 API 不兼容 | 中 | 低 | WireMock 仅在测试中使用，如遇到不兼容的 API 可单独调整测试代码 |
| Rebase main 时 `build-parent/pom.xml` 持续冲突 | 高 | 中 | 尽量减少不必要的版本变量变更；只改与降级直接相关的版本属性 |
| `jakarta.*` 版本在 SB 2.7 BOM 和 Timefold 现有版本间可能冲突 | 中 | 中 | 对 JAXB/JPA/CDI 等使用显式版本覆盖，不依赖 SB BOM 管理这些 Jakarta API 版本 |
| Testcontainers 大版本差异（2.0.5 → 1.17.x） | 中 | 中 | 仅在少数集成测试中使用，如遇到不兼容需逐个适配 |

## Open Questions

- **WireMock 版本**: 当前使用 3.13.2，SB 2.7 不管理 WireMock。是否需要显式降级到 2.x 版本？还是可以保留 3.x（WireMock 3 需要 Java 11+，与 Java 17 兼容）？
- **Logback 版本冲突风险**: SB 2.7.18 管理 logback 1.2.12，当前 Timefold 声明了 1.5.34。降级后是否会有 SLF4J 绑定兼容问题？
- **Testcontainers 兼容性**: SB 2.7.18 BOM 管理的是旧版 Testcontainers，而当前代码使用 2.0.5 版本。是否应该保持较新版本（手动覆盖）？
