package com.jober.final2teamdrhong.dto.recipient;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.Length;

/**
 * 수신자(Recipient) 관련 요청 DTO들을 모아두는 클래스입니다.
 */
public class RecipientRequest {

    /**
     * 수신자 생성을 위한 요청 DTO 입니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "RecipientCreateDTO")
    public static class CreateDTO {
        @NotBlank(message = "수신인 이름은 필수 입력 항목입니다.")
        @Length(min = 2, max = 10, message = "수신인 이름은 2자 이상 10자 이하로 입력해주세요.")
        private String recipientName;
        @NotBlank(message = "수신인 연락처는 필수 입력 항목입니다.")
        private String recipientPhoneNumber;
        @Length(max = 1000, message = "메모는 1000자 이하로 입력해주세요.")
        private String recipientMemo;
    }

    /**
     * 수신자 정보 수정을 위한 요청 DTO 입니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "RecipientUpdateDTO")
    public static class UpdateDTO {
        @NotBlank(message = "수신인 이름은 필수 입력 항목입니다.")
        @Length(min = 2, max = 10, message = "수신인 이름은 2자 이상 10자 이하로 입력해주세요.")
        private String newRecipientName;
        @NotBlank(message = "수신인 연락처는 필수 입력 항목입니다.")
        private String newRecipientPhoneNumber;
        @Length(max = 1000, message = "메모는 1000자 이하로 입력해주세요.")
        private String newRecipientMemo;
    }
}
