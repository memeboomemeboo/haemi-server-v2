package com.memeboo2.haemi.elder.response.presentation.dto;

import com.memeboo2.haemi.elder.response.domain.Emotion;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EmotionRequest(
        @NotEmpty @Size(max = 2) List<Emotion> emotions
) {}
