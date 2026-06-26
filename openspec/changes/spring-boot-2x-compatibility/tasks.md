## 1. 构建基础层：依赖版本降级

- [x] 1.1 修改 `build/build-parent/pom.xml`：将 `version.org.springframework.boot` 从 4.1.0 改为 2.7.18
- [x] 1.2 修改 `build/build-parent/pom.xml`：将 `maven.compiler.release` 从 21 改为 17
- [ ] 1.3 修改 `build/build-parent/pom.xml`：移除 `version.tools.jackson` 属性（`3.2.0`），添加或确认 `version.com.fasterxml.jackson.core` 由 SB 2.7 BOM 管理（2.13.5）
- [ ] 1.4 修改 `build/build-parent/pom.xml`：将 `version.org.junit.jupiter` 从 6.1.0 移除（交由 SB BOM 管理 5.8.2）
- [ ] 1.5 修改 `build/build-parent/pom.xml`：移除 `junit-bom` 的显式版本导入（交由 SB BOM 管理）
- [ ] 1.6 修改 `build/build-parent/pom.xml`：检查并确认 `jakarta.*` 相关版本属性不受 SB BOM 影响，保持 Timefold 自定义版本
- [ ] 1.7 修改 `build/build-parent/pom.xml`：移除 `enforce-managed-deps-rule` 中对 Jackson 3 版本的约束（如存在）
- [ ] 1.8 运行 `mvn validate` 验证根 POM 结构正确，无 XML 错误

## 2. Jackson 3 → Jackson 2 降级

- [ ] 2.1 修改 `persistence/jackson/pom.xml`：将 `tools.jackson:jackson-bom` → `com.fasterxml.jackson:jackson-bom`（版本由 SB 2.7 BOM 管理，移除显式版本）
- [ ] 2.2 修改 `persistence/jackson/pom.xml`：将所有 `tools.jackson.core:*` → `com.fasterxml.jackson.core:*`
- [ ] 2.3 批量替换 `persistence/jackson/src/main/java/` 下所有 Java 文件的 import：`tools.jackson.*` → `com.fasterxml.jackson.*`
- [ ] 2.4 适配 `TimefoldJacksonModule.java`：`tools.jackson.databind.JacksonModule` → `com.fasterxml.jackson.databind.Module`
- [ ] 2.5 检查并适配 `JacksonModule.createModule()` / `SimpleModule` 等 API 在 Jackson 2 中的签名差异
- [ ] 2.6 批量替换 `persistence/jackson/src/test/java/` 下所有 Java 文件的 import：`tools.jackson.*` → `com.fasterxml.jackson.*`
- [ ] 2.7 修改 `persistence/jackson/src/main/java/module-info.java`：更新 Jackson 相关的 `requires` 和 `provides` 语句
- [ ] 2.8 修改 `spring-integration/pom.xml`：将 `tools.jackson:jackson-bom` → `com.fasterxml.jackson:jackson-bom`
- [ ] 2.9 修改 `spring-integration/spring-boot-autoconfigure/pom.xml`：将 `tools.jackson.core:jackson-databind` → `com.fasterxml.jackson.core:jackson-databind` (optional)
- [ ] 2.10 修改 `TimefoldSolverBeanFactory.java`：将 `tools.jackson.databind.JacksonModule` → `com.fasterxml.jackson.databind.Module`，`tools.jackson.databind.json.JsonMapper` → `com.fasterxml.jackson.databind.json.JsonMapper`
- [ ] 2.11 修改 `spring-integration/spring-boot-autoconfigure/src/main/java/module-info.java`：更新 `requires tools.jackson.databind` → `requires com.fasterxml.jackson.databind`

## 3. 移除 AOT 编译代码

- [ ] 3.1 删除 `TimefoldSolverAotContribution.java` 文件
- [ ] 3.2 将 `TimefoldSolverAotFactory.java` 重命名为 `SolverConfigFactory.java`，移除 `EnvironmentAware` 接口（改为构造函数注入）
- [ ] 3.3 重构 `TimefoldSolverAutoConfiguration.java`：移除 `implements BeanFactoryInitializationAotProcessor`
- [ ] 3.4 重构 `TimefoldSolverAutoConfiguration.java`：移除 `processAheadOfTime()` 方法
- [ ] 3.5 重构 `TimefoldSolverAutoConfiguration.java`：将 `postProcessBeanDefinitionRegistry()` 中的 Bean 注册逻辑改为 `@Bean` 方法（SolverConfig、SolverManager 的 Bean 定义）
- [ ] 3.6 重构 `TimefoldSolverAutoConfiguration.java`：移除 `import org.springframework.beans.factory.aot.*`
- [ ] 3.7 修改 `TimefoldSolverAutoConfiguration.java`：移除 `NativeDetector.inNativeImage()` 相关分支（保留 NativeDetector import 但简化 native image 特殊处理，因为 SB 2.7 中不支持 AOT 原生镜像）
- [ ] 3.8 修改 `TimefoldSolverBeanFactory.java`：移除 AOT 相关 import 和注释引用
- [ ] 3.9 修改 `TimefoldSolverConstraintAutoConfigurationTest.java`：移除 `Mockito.mockStatic(NativeDetector.class)` 的测试

## 4. spring-boot-persistence 替换

- [ ] 4.1 修改 `spring-integration/spring-boot-autoconfigure/pom.xml`：移除 `spring-boot-persistence` 依赖，确认 `spring-boot-autoconfigure` 依赖已存在
- [ ] 4.2 修改 `spring-integration/spring-boot-starter/pom.xml`：移除 `spring-boot-persistence` 依赖
- [ ] 4.3 修改 `IncludeAbstractClassesEntityScanner.java`：将 `import org.springframework.boot.persistence.autoconfigure.EntityScanPackages` → `import org.springframework.boot.autoconfigure.domain.EntityScanPackages`
- [ ] 4.4 修改 `IncludeAbstractClassesEntityScanner.java`：将 `import org.springframework.boot.persistence.autoconfigure.EntityScanner` → `import org.springframework.boot.autoconfigure.domain.EntityScanner`
- [ ] 4.5 修改 `TimefoldSolverAutoConfiguration.java`：将 `import org.springframework.boot.persistence.autoconfigure.EntityScan` → `import org.springframework.boot.autoconfigure.domain.EntityScan`
- [ ] 4.6 修改 `spring-integration/spring-boot-autoconfigure/src/main/java/module-info.java`：移除 `requires spring.boot.persistence`
- [ ] 4.7 批量替换所有测试类中的 `import org.springframework.boot.persistence.autoconfigure.EntityScan` → `import org.springframework.boot.autoconfigure.domain.EntityScan`（约 25 个文件）
- [ ] 4.8 批量替换所有测试类中的 `import org.springframework.boot.persistence.autoconfigure.AutoConfigurationPackage` → 确认 `@AutoConfigurationPackage` 在 SB 2.7 中的包路径（`org.springframework.boot.autoconfigure.AutoConfigurationPackage`）

## 5. 自动配置注册机制迁移

- [ ] 5.1 在 `spring-integration/spring-boot-autoconfigure/src/main/resources/META-INF/` 下创建 `spring.factories`
- [ ] 5.2 `spring.factories` 内容：`org.springframework.boot.autoconfigure.EnableAutoConfiguration` 键，注册 `TimefoldSolverAutoConfiguration`、`TimefoldBenchmarkAutoConfiguration`、`TimefoldSolverBeanFactory`
- [ ] 5.3 保留现有的 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件（SB 2.7 会自动忽略）

## 6. Native Image 配置清理

- [ ] 6.1 修改 `spring-integration/spring-boot-integration-test/pom.xml`：移除 native profile 中的 `<goal>process-aot</goal>` 和 `<goal>process-test-aot</goal>`
- [ ] 6.2 确认 native profile 中的 `graalvm.buildtools:native-maven-plugin` 配置调整为与 SB 2.7 兼容（可选：如果用户不需要 native image 可整体移除 native profile）
- [ ] 6.3 保留 `META-INF/native-image/` 下的 reflect-config.json、proxy-config.json、resource-config.json（GraalVM 原生反射配置仍有用）

## 7. 依赖版本全面对齐

- [ ] 7.1 检查 `build/build-parent/pom.xml` 中 `version.org.wiremock`（3.13.2）是否需要降级为 SB 2.7 兼容版本
- [ ] 7.2 检查 `build/build-parent/pom.xml` 中 `version.ch.qos.logback`（1.5.34）与 SB 2.7 BOM（1.2.12）的兼容性，决定保留自定义版本还是使用 BOM 版本
- [ ] 7.3 检查 `build/build-parent/pom.xml` 中 `version.org.testcontainers`（2.0.5）是否需要降级，SB 2.7 BOM 管理 1.17.x
- [ ] 7.4 确认 `version.org.assertj`（3.27.7）是否需要降级，SB 2.7 BOM 管理 3.22.0
- [ ] 7.5 确认 `version.org.awaitility`（4.3.0）是否需要降级，SB 2.7 BOM 管理 4.2.0
- [ ] 7.6 确认 `version.org.mockito`（由 SB BOM 管理）在 SB 2.7 BOM 中为 4.5.1，检查测试代码兼容性
- [ ] 7.7 修改 `build/build-parent/pom.xml` 中 `version.com.fasterxml.jackson.core`（2.22）→ 移除显式版本，交由 SB 2.7 BOM 管理（2.13.5）
- [ ] 7.8 修改 `build/build-parent/pom.xml` 中 `version.com.fasterxml.jackson.core:jackson-annotations` 显式声明为可选或移除
- [ ] 7.9 确认 `jakarta.xml.bind-api`、`jakarta.persistence-api` 等 Jakarta API 的显式版本覆盖与 SB 2.7 BOM 中同名依赖不冲突（Timefold 版本优先级更高）

## 8. 完整编译验证

- [ ] 8.1 运行 `mvn clean compile -pl core` 验证 core 模块编译通过
- [ ] 8.2 运行 `mvn clean compile -pl persistence/jackson` 验证 Jackson 模块编译通过
- [ ] 8.3 运行 `mvn clean compile -pl persistence/jaxb` 验证 JAXB 模块编译通过
- [ ] 8.4 运行 `mvn clean compile -pl persistence/jpa` 验证 JPA 模块编译通过
- [ ] 8.5 运行 `mvn clean compile -pl spring-integration` 验证 Spring 集成模块编译通过
- [ ] 8.6 运行 `mvn clean compile -pl service` 验证 Service 模块编译通过
- [ ] 8.7 运行 `mvn clean compile -pl tools` 验证 Tools 模块编译通过
- [ ] 8.8 运行 `mvn clean compile` 验证全量编译通过

## 9. 测试验证

- [ ] 9.1 运行 `mvn test -pl persistence/jackson` 验证 Jackson 序列化 round-trip 测试全部通过
- [ ] 9.2 运行 `mvn test -pl spring-integration/spring-boot-autoconfigure` 验证所有 Spring 自动配置测试通过
- [ ] 9.3 运行 `mvn test -pl core` 验证核心引擎测试通过
- [ ] 9.4 运行 `mvn test -pl persistence/jaxb,persistence/jpa` 验证持久化模块测试通过
- [ ] 9.5 定位并修复所有因依赖版本变化导致的编译错误或测试失败

## 10. 代码格式和清理

- [ ] 10.1 运行 `mvn spotless:apply` 统一代码格式
- [ ] 10.2 确认 `mvn enforcer:enforce` 通过（无重复类冲突、无管理依赖违规）
- [ ] 10.3 提交代码，commit message 涵盖所有降级变更
