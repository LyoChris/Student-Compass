package org.backendcompas.core.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean(name = "flyway", initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor flywayDependsOnEntityManagerFactoryPostProcessor() {
        return beanFactory -> {
            if (beanFactory.containsBeanDefinition("entityManagerFactory")) {
                BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
                entityManagerFactory.setDependsOn(appendDependency(entityManagerFactory.getDependsOn(), "flyway"));
            }
        };
    }

    private static String[] appendDependency(String[] currentDependencies, String dependency) {
        if (currentDependencies == null || currentDependencies.length == 0) {
            return new String[]{dependency};
        }

        String[] dependencies = new String[currentDependencies.length + 1];
        System.arraycopy(currentDependencies, 0, dependencies, 0, currentDependencies.length);
        dependencies[currentDependencies.length] = dependency;
        return dependencies;
    }
}
