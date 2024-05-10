package com.zj.codestaging.config.%moduleName%;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class %classPrefix%WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("子模块 [%moduleName%] 加载了自定义静态资源路径");
        registry.addResourceHandler("/%moduleName%/**").addResourceLocations("classpath:/static/%moduleName%/");
    }
}
