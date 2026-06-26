## Why

当前 Timefold Solver 基于 Spring Boot 4.1.0 和 Java 21 构建，而大量企业生产环境仍运行在 Spring Boot 2.7.x + Java 17 生态上。此次降级旨在让 Timefold Solver 能够在 Spring Boot 2.7.18 环境中正常编译、运行和测试，使存量企业用户无需升级 Spring Boot 大版本即可使用最新版本的 Timefold Solver。

## What Changes

- **Spring Boot 版本降级**: 4.1.0 → 2.7.18，连带 Spring Framework 7.x → 5.3.31
- **Java 编译版本降级**: 21 → 17
- **Jackson 版本降级**: Jackson 3 (`tools.jackson`) → Jackson 2 (`com.fasterxml.jackson`)，影响 `persistence/jackson/` 整个模块和 `spring-integration/` 中的相关自动配置
- **移除 AOT 编译支持**: 删除 `BeanFactoryInitializationAotContribution`、`BeanFactoryInitializationAotProcessor` 相关代码，这些是 Spring Framework 6+ 才有的 API
- **替换 spring-boot-persistence 依赖**: SB 2.7 中不存在 `spring-boot-persistence` 模块，`EntityScan`/`EntityScanner`/`EntityScanPackages` 改为从 `spring-boot-autoconfigure` 中引入（包路径从 `o.s.b.persistence.autoconfigure` → `o.s.b.autoconfigure.domain`）
- **自动配置注册机制迁移**: `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`（SB 3.0+）→ `META-INF/spring.factories`（SB 2.7）
- **JUnit 版本降级**: 6.1.0 → 由 SB 2.7 BOM 管理（5.8.2），移除显式版本声明
- **关联依赖版本对齐**: WireMock、Testcontainers、Logback、Micrometer 等版本随 SB 2.7 BOM 调整
- **JPMS module-info.java 更新**: 移除 `requires spring.boot.persistence`，更新 Jackson module require 语句

## Capabilities

### New Capabilities

- `spring-boot-2x-autoconfiguration`: Spring Boot 2.7.x 自动配置支持，包括 SolverFactory/SolverManager Bean 自动装配、多求解器配置、基准测试自动配置
- `jackson2-integration`: 基于 Jackson 2 的 Score 序列化/反序列化、SolutionFileIO、TimefoldJacksonModule 注册

### Modified Capabilities

（无——当前不存在已有 specs，本次为全新定义）

## Impact

| 影响范围 | 说明 |
|----------|------|
| `build/build-parent/pom.xml` | 版本属性变更（SB、Spring Framework、Jackson、JUnit、Java 编译器版本等） |
| `spring-integration/pom.xml` | Jackson BOM 和 Spring Boot BOM 导入替换 |
| `spring-integration/spring-boot-autoconfigure/` | AOT 代码移除、EntityScan 包路径迁移、Jackson 2 适配、spring.factories 创建（约 8 个主代码文件 + 30+ 测试文件） |
| `spring-integration/spring-boot-starter/` | 移除 spring-boot-persistence 依赖、module-info 更新 |
| `spring-integration/spring-boot-integration-test/` | 移除 native profile 中的 process-aot goal |
| `persistence/jackson/` | 全部约 60 个 Java 文件：`tools.jackson.*` → `com.fasterxml.jackson.*` 导入替换、API 适配 |
| `core/` | 无改动（核心引擎独立于 Spring 版本） |
| `persistence/jaxb/`, `persistence/jpa/` | 无改动（jakarta.* 依赖独立管理，不受 Spring 版本影响） |
| `service/` | 无改动（基于 Quarkus/CDI，独立于 Spring 版本） |
| `quarkus-integration/` | 无改动 |
| `tools/` | 无改动 |
