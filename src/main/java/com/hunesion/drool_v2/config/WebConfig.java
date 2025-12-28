package com.hunesion.drool_v2.config;

import com.hunesion.drool_v2.interceptor.AccessControlInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebConfig - Registers the AccessControlInterceptor
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AccessControlInterceptor accessControlInterceptor;

    @Autowired
    public WebConfig(AccessControlInterceptor accessControlInterceptor) {
        this.accessControlInterceptor = accessControlInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessControlInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/public/**",
                        "/error"
                );
    }
}
