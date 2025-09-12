package com.jober.final2teamdrhong.exception;

import lombok.Getter;

// 에러 응답을 위한 간단한 DTO 클래스
@Getter
public class ErrorResponse {
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }
}