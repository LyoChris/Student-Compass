package org.backendcompas.core.config;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayConfigTest {

    @Test
    void buildsFlywayInstance() {
        FlywayConfig config = new FlywayConfig();
        DataSource dataSource = Mockito.mock(DataSource.class);

        Flyway flyway = config.flyway(dataSource);

        assertThat(flyway).isNotNull();
    }

    @Test
    void setsFlywayDependencyWhenEntityManagerFactoryPresent() {
        BeanFactoryPostProcessor processor = FlywayConfig.flywayDependsOnEntityManagerFactoryPostProcessor();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        RootBeanDefinition definition = new RootBeanDefinition(Object.class);
        beanFactory.registerBeanDefinition("entityManagerFactory", definition);

        processor.postProcessBeanFactory(beanFactory);

        String[] dependsOn = beanFactory.getBeanDefinition("entityManagerFactory").getDependsOn();
        assertThat(dependsOn).contains("flyway");
    }

    @Test
    void appendsFlywayDependencyWhenExistingDependsOnPresent() {
        BeanFactoryPostProcessor processor = FlywayConfig.flywayDependsOnEntityManagerFactoryPostProcessor();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        RootBeanDefinition definition = new RootBeanDefinition(Object.class);
        definition.setDependsOn("other");
        beanFactory.registerBeanDefinition("entityManagerFactory", definition);

        processor.postProcessBeanFactory(beanFactory);

        String[] dependsOn = beanFactory.getBeanDefinition("entityManagerFactory").getDependsOn();
        assertThat(dependsOn).contains("other", "flyway");
    }

    @Test
    void leavesBeanFactoryWhenEntityManagerFactoryMissing() {
        BeanFactoryPostProcessor processor = FlywayConfig.flywayDependsOnEntityManagerFactoryPostProcessor();
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

        processor.postProcessBeanFactory(beanFactory);

        assertThat(beanFactory.containsBeanDefinition("entityManagerFactory")).isFalse();
    }
}
