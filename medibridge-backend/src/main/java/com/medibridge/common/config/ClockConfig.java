package com.medibridge.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The system clock, as an injectable bean.
 *
 * <p>Introduced for {@code LiveQueueService}, whose entire output is a function
 * of the current time: a queue position, an ETA and a "running late" figure are
 * meaningless without a now to measure from. Calling {@code LocalDateTime.now()}
 * there would make its behaviour depend on what time of day the test suite
 * happened to run - a scenario needing a slot an hour before now and another an
 * hour after cannot even be expressed near midnight, and the delay branches key
 * off exactly that ordering.
 *
 * <p>Deliberately not a sweeping change: the rest of the codebase calls
 * {@code now()} directly and there is no reason to churn it. This exists where
 * time <em>is</em> the logic.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
