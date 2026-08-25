package com.memeboo2.haemi.platform.content.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ContentPolicyProperties.class)
public class ContentConfig {}
