package com.zj.codestaging.config.%moduleName%;

import org.apache.catalina.session.DataSourceStore;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.lookup.DataSourceLookup;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@MapperScan(
        basePackages = {"com.zj.codestaging.mapper.%moduleName%"},
        sqlSessionFactoryRef = "%moduleName%SqlSessionFactory"
)

public class %classPrefix%DatasourceConfig {
    /**
     * 寻找配置文件中的数据源配置信息
     * @return
     */
    @Primary
    @Bean("%moduleName%DatasourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.%moduleName%")
    public DataSourceProperties dataSourceProperties(){
        return new DataSourceProperties();
    }

    /**
     * 构建数据源
     * @param properties
     * @return
     */
    @Primary
    @Bean("%moduleName%Datasource")
    public DataSource dataSource(@Qualifier("%moduleName%DatasourceProperties") DataSourceProperties properties){
        DataSource build = null;

        build = DataSourceBuilder.create()
            .driverClassName(properties.getDriverClassName())
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();

        return build;
    }

    /**
     * 构建事务管理器
     * @param dataSource
     * @return
     */
    @Primary
    @Bean(name = "%moduleName%TransactionManager")
    public PlatformTransactionManager dataSourceTransactionManager(@Qualifier("%moduleName%Datasource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 构建sqlSession工厂
     * @param dataSource
     * @return
     * @throws Exception
     */
    @Bean(name = "%moduleName%SqlSessionFactory")
    public SqlSessionFactory sqlSessionFactory(@Qualifier("%moduleName%Datasource") DataSource dataSource) throws Exception {
        final SqlSessionFactoryBean sessionFactoryBean = new SqlSessionFactoryBean();
        sessionFactoryBean.setDataSource(dataSource);
        sessionFactoryBean.setTypeHandlersPackage("com.zj.codestaging.entity.%moduleName%");
        // TODO 设置子模块的xml路径
        sessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/%moduleName%/*.xml"));
        return sessionFactoryBean.getObject();
    }

    /**
     * 构建sqlSession
     * @param sqlSessionFactory
     * @return
     */
    @Primary
    @Bean(name = "%moduleName%SqlSessionTemplate")
    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("%moduleName%SqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
