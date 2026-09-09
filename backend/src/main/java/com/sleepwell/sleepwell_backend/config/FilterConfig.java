package com.sleepwell.sleepwell_backend.config;

import com.sleepwell.sleepwell_backend.config.filter.HostHeaderValidationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 서블릿 필터 설정
 *
 * 등록된 필터:
 * - HostHeaderValidationFilter: Host 헤더 검증 (우선순위 1)
 *
 * @author SleepWell Development Team
 * @since 1.0
 */
@Configuration
public class FilterConfig {

    /**
     * Host 헤더 검증 필터 등록
     *
     * 우선순위를 최상위(HIGHEST_PRECEDENCE)로 설정하여
     * 다른 필터(인증, CORS 등)보다 먼저 실행되도록 함
     *
     * @return FilterRegistrationBean<HostHeaderValidationFilter>
     */
    @Bean
    public FilterRegistrationBean<HostHeaderValidationFilter> hostHeaderValidationFilter() {
        FilterRegistrationBean<HostHeaderValidationFilter> registrationBean =
                new FilterRegistrationBean<>();

        registrationBean.setFilter(new HostHeaderValidationFilter());
        registrationBean.addUrlPatterns("/*");  // 모든 경로에 적용
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);  // 최우선 실행
        registrationBean.setName("hostHeaderValidationFilter");

        return registrationBean;
    }
}
