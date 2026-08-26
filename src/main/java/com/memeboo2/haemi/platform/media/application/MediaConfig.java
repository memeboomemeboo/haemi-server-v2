package com.memeboo2.haemi.platform.media.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({UploadPolicyProperties.class, HeicProperties.class})
public class MediaConfig {
}
