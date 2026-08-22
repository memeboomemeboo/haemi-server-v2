package com.memeboo2.haemi.elder.response.presentation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TextRequest(
        @NotBlank @Size(max = 100) String text
) {}
