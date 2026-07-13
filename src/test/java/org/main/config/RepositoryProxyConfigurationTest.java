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
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class RepositoryProxyConfigurationTest {

    @Test
    void usesJdkProxyForFinalRepositoryImplementation() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);

        new ApplicationContextRunner().
                withInitializer(new ConfigDataApplicationContextInitializer()).
                withConfiguration(AutoConfigurations.of(
                        AopAutoConfiguration.class,
                        PersistenceExceptionTranslationAutoConfiguration.class)).
                withBean(JdbcTemplate.class, () -> jdbc).
                withBean(JdbcBuildAssetRepository.class,
                        () -> new JdbcBuildAssetRepository(jdbc)).
                run(context -> {
                    BuildAssetRepository repository = context.getBean(
                            BuildAssetRepository.class);

                    assertThat(AopUtils.isJdkDynamicProxy(repository)).isTrue();
                });
    }
}
