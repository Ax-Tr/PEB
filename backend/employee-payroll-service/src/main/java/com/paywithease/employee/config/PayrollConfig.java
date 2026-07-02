package com.paywithease.employee.config;

import com.paywithease.employee.domain.payroll.PayrollRates;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the statutory {@link PayrollRates}. Development defaults are used here; in production
 * these are sourced from the Rules Engine (state/industry-scoped) and CA-verified.
 */
@Configuration
public class PayrollConfig {

  @Bean
  @ConditionalOnMissingBean
  public PayrollRates payrollRates() {
    return PayrollRates.defaults();
  }
}
