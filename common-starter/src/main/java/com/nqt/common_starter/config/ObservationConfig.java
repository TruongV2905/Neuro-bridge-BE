package com.nqt.common_starter.config;

import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Configuration
public class ObservationConfig {
    @PostConstruct
    void setup() {
        // Tự động chuyển tiếp RequestAttributes (chứa các Header)
        ContextRegistry.getInstance()
                .registerThreadLocalAccessor(new RequestAttributesAccessor());
    }
}
class RequestAttributesAccessor implements ThreadLocalAccessor<RequestAttributes> {
    @Override
    public Object key() { return RequestAttributesAccessor.class.getName(); }

    @Override
    public RequestAttributes getValue() { return RequestContextHolder.getRequestAttributes(); }

    @Override
    public void setValue(RequestAttributes value) { RequestContextHolder.setRequestAttributes(value); }

    @Override
    public void reset() { RequestContextHolder.resetRequestAttributes(); }
}
