package com.homework.driveman.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 — 跨域访问 + 静态资源映射
 */
@Slf4j
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    /** 文件上传根目录（与 application.yaml 中 drive.upload.path 保持一致） */
    @Value("${drive.upload.path:./upload-files}")
    private String uploadPath;

    @Autowired
    private JwtInterceptor jwtInterceptor;

    public WebMvcConfiguration() {
        log.debug("创建配置对象: WebMvcConfiguration");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/register",
                        "/doc.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/uploads/**"
                );
        log.debug("注册 JWT 拦截器，排除: /login, /doc.html, /swagger-ui/**, /v3/api-docs/**, /webjars/**, /uploads/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedHeaders("*")
                .allowedMethods("*")
                .allowedOriginPatterns("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 将上传目录映射为静态资源，可通过 /uploads/** 直接访问
     * 例如文件存储在 ./upload-files/id_card_front/1_xxx.jpg
     * 则访问 http://localhost:9080/uploads/id_card_front/1_xxx.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath + "/");
        log.debug("静态资源映射: /uploads/** -> file:{}/", uploadPath);
    }
}
