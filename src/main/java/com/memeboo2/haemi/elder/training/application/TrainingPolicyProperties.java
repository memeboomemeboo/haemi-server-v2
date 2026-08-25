package com.memeboo2.haemi.elder.training.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "haemi.training")
public record TrainingPolicyProperties(
        @DefaultValue("3") int orientationQuestionCount,
        @DefaultValue("3") int recallQuestionCount,
        @DefaultValue("2") int languageQuestionCount,
        @DefaultValue("2") int delayedRecallQuestionCount,
        @DefaultValue("90") int inactivityReminderSeconds,
        @DefaultValue("0.8") double promotionAccuracy,
        @DefaultValue("0.4") double demotionAccuracy,
        @DefaultValue("2") int promotionConsecutiveDays
) {
    public int totalQuestionCount() {
        return orientationQuestionCount + recallQuestionCount + languageQuestionCount + delayedRecallQuestionCount;
    }
}
