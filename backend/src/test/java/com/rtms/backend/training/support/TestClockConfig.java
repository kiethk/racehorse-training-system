package com.rtms.backend.training.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Thay đồng hồ thật bằng MutableClock trong test nhập qua @Import. MutableClock vừa là Clock nên các service nhận nó qua kiểu Clock.
 * (Swaps the real clock for a MutableClock in tests that @Import it. A MutableClock is a Clock, so services receive it as a Clock.)
 */
@TestConfiguration
public class TestClockConfig {

    @Bean
    @Primary
    public MutableClock testClock() {
        return new MutableClock();
    }
}
