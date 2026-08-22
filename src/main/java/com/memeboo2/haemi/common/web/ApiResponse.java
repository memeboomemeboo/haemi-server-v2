package com.memeboo2.haemi.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(T data, ErrorBody error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null);
    }

    public static ApiResponse<Void> error(String code, String message, String field) {
        return new ApiResponse<>(null, new ErrorBody(code, message, field));
    }

    public static ApiResponse<Void> error(String code, String message) {
        return error(code, message, null);
    }

    public record ErrorBody(String code, String message, String field) {}
}
