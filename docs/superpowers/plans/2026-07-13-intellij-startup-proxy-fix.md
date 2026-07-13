# IntelliJ Startup Proxy Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the Spring Boot application start from IntelliJ IDEA without CGLIB failing on final champion-build repository classes.

**Architecture:** Keep the final repository implementations and their existing interfaces. Configure Spring AOP to use JDK interface proxies, prove the real application property lets a final `@Repository` bean initialize, then run the application from the repository root.

**Tech Stack:** Java 23, Spring Boot 3.3.3, JUnit 5, AssertJ, Maven

## Global Constraints

- Do not remove `final` from repository implementations.
- Do not remove `@Repository` or Spring exception translation.
- Do not add dependencies or IntelliJ-specific project files.
- Keep the production change to the Spring AOP proxy configuration.

---

### Task 1: Use Interface Proxies for Final Repositories

**Files:**
- Create: `src/test/java/org/main/config/RepositoryProxyConfigurationTest.java`
- Modify: `src/main/resources/application.properties`

**Interfaces:**
- Consumes: `BuildAssetRepository`, `JdbcBuildAssetRepository(JdbcTemplate)`, and Spring Boot's `spring.aop.proxy-target-class` property.
- Produces: a startable `BuildAssetRepository` JDK proxy while retaining the final concrete implementation.

- [ ] **Step 1: Write the failing context regression**

```java
package org.main.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.main.builds.api.BuildAssetRepository;
import org.main.builds.api.JdbcBuildAssetRepository;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.boot.context.config.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

class RepositoryProxyConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().
            withInitializer(new ConfigDataApplicationContextInitializer()).
            withConfiguration(AutoConfigurations.of(
                    AopAutoConfiguration.class,
                    PersistenceExceptionTranslationAutoConfiguration.class)).
            withUserConfiguration(RepositoryConfiguration.class);

    @Test
    void finalRepositoryStartsThroughItsInterfaceProxy() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(AopUtils.isJdkDynamicProxy(
                    context.getBean(BuildAssetRepository.class))).isTrue();
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class RepositoryConfiguration {

        @Bean
        JdbcTemplate jdbcTemplate() {
            return mock(JdbcTemplate.class);
        }

        @Bean
        BuildAssetRepository buildAssetRepository(JdbcTemplate jdbcTemplate) {
            return new JdbcBuildAssetRepository(jdbcTemplate);
        }
    }
}
```

- [ ] **Step 2: Run the regression and verify RED**

Run:

```powershell
./mvnw.cmd -Dtest=RepositoryProxyConfigurationTest test
```

Expected: context startup fails with `Cannot subclass final class org.main.builds.api.JdbcBuildAssetRepository`.

- [ ] **Step 3: Add the minimal production configuration**

Add to `src/main/resources/application.properties`:

```properties
spring.aop.proxy-target-class=false
```

- [ ] **Step 4: Run the focused regression and verify GREEN**

Run:

```powershell
./mvnw.cmd -Dtest=RepositoryProxyConfigurationTest test
```

Expected: one test passes with zero failures and the bean is a JDK dynamic proxy.

- [ ] **Step 5: Run broader verification**

Run:

```powershell
./mvnw.cmd test
```

Expected: the full non-Testcontainers suite passes and Checkstyle reports zero violations.

- [ ] **Step 6: Verify real startup**

Run `org.main.RiotPractice` with Java 23 and working directory `D:\Games\RiotApiPractice`, matching the IntelliJ run configuration.

Expected: the application passes repository creation and listens on its configured HTTP port. If another independent startup error appears, capture it as a separate issue.

- [ ] **Step 7: Commit when Git index access is available**

```powershell
git add src/main/resources/application.properties src/test/java/org/main/config/RepositoryProxyConfigurationTest.java docs/superpowers/specs/2026-07-13-intellij-startup-proxy-design.md docs/superpowers/plans/2026-07-13-intellij-startup-proxy-fix.md
git commit -m "fix: allow final repositories to use interface proxies"
```
