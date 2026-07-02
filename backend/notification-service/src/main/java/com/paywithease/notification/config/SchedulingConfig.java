package com.paywithease.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the {@code @Scheduled} reminder sender. */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
