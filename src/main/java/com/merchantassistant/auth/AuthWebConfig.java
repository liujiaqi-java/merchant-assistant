package com.merchantassistant.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.merchantassistant.store.StoreInterceptor;

@Configuration
public class AuthWebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final OperationLogInterceptor operationLogInterceptor;
    private final StoreInterceptor storeInterceptor;
    public AuthWebConfig(AuthInterceptor authInterceptor, StoreInterceptor storeInterceptor, OperationLogInterceptor operationLogInterceptor) { this.authInterceptor = authInterceptor; this.storeInterceptor = storeInterceptor; this.operationLogInterceptor = operationLogInterceptor; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(authInterceptor); registry.addInterceptor(storeInterceptor); registry.addInterceptor(operationLogInterceptor); }
}
