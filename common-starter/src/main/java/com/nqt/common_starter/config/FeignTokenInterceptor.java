package com.nqt.common_starter.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Slf4j
public class FeignTokenInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();

            String[] headersToRelay = {
                    "X-User-Id",
                    "X-User-Roles",
                    "X-User-Permissions",
                    "X-Signature"
            };

            for (String header : headersToRelay) {
                String value = request.getHeader(header);
                if (value != null) {
                    template.header(header, value);
                    // Log debug để kiểm tra headers đã được relay thành công chưa
                    log.debug("Relaying header {}: {}", header, value);
                }
            }
        } else {
            log.warn("No RequestAttributes found. Headers will not be relayed to Feign request.");
        }
    }
}
