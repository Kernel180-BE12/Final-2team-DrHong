package com.jober.final2teamdrhong.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 애플리케이션에서 발생한 예외 정보를 클라이언트에 전달하는 오류 응답 DTO.
 * 단일 메시지 필드를 포함하며 전역 예외 처리기에서 공통적으로 사용한다.
 */
@Getter
@Schema(description = "오류 응답 정보")
public class ErrorResponse {
    @Schema(description = "오류 메시지", example = "서비스를 일시적으로 이용할 수 없습니다. 잠시 후 다시 시도해주세요.")
    private final String message;

    // Rate Limiting 전용 필드 (선택적으로 포함)
    private final Long retryAfterSeconds;

    /**
     * 오류 응답 객체를 생성한다.
     *
     * @param message 사용자에게 전달할 오류 메시지
     */
    public ErrorResponse(String message) {
        this.message = message;
        this.retryAfterSeconds = null;
    }

    // Rate Limiting 에러 응답용 생성자 (retryAfterSeconds 포함)
    public ErrorResponse(String message, Long retryAfterSeconds) {
        this.message = message;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}