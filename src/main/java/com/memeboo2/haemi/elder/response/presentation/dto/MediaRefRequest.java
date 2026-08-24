package com.memeboo2.haemi.elder.response.presentation.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MediaRefRequest(@NotNull UUID mediaRefId) {}
