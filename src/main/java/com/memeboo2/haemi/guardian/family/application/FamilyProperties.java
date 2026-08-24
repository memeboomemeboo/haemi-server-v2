package com.memeboo2.haemi.guardian.family.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "haemi.family")
public record FamilyProperties(int maxElders, int maxGuardians) {

    public FamilyProperties {
        if (maxElders <= 0) maxElders = 4;
        if (maxGuardians <= 0) maxGuardians = 8;
    }
}
