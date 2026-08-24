package com.memeboo2.haemi.auth.verification.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PhoneVerificationProperties.class)
public class PhoneVerificationConfig {}
