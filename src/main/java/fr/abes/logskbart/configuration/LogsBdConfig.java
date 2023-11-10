package fr.abes.logskbart.configuration;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(entityManagerFactoryRef = "logsEntityManager", transactionManagerRef = "logsTransactionManager", basePackages = "fr.abes.logskbart.repository")
@NoArgsConstructor
@LogsBdConfiguration
public class LogsBdConfig extends AbstractConfig {
    @Value("${spring.jpa.logsdb.show-sql}")
    protected boolean showsql;
    @Value("${spring.jpa.logsdb.properties.hibernate.dialect}")
    protected String dialect;
    @Value("${spring.jpa.logsdb.hibernate.ddl-auto}")
    protected String ddlAuto;
    @Value("${spring.jpa.logsdb.database-platform}")
    protected String platform;
    @Value("${spring.jpa.logsdb.generate-ddl}")
    protected boolean generateDdl;
    @Value("${spring.sql.logsdb.init.mode}")
    protected String initMode;

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.logsdb")
    public DataSource logsDataSource() { return DataSourceBuilder.create().build(); }

    @Bean
    public LocalContainerEntityManagerFactoryBean logsEntityManager() {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(logsDataSource());
        em.setPackagesToScan(new String[]{"fr.abes.logskbart.entity"});
        configHibernate(em, platform, showsql, dialect, ddlAuto, generateDdl, initMode);
        return em;
    }

    @Bean
    public PlatformTransactionManager logsTransactionManager(@Qualifier("logsEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactory) {
        final JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory.getObject());
        return transactionManager;
    }
}
