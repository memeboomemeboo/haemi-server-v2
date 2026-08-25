package com.memeboo2.haemi.elder.training.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TrainingPolicyProperties.class)
public class TrainingConfig {}
