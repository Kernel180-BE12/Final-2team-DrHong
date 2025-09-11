package com.jober.final2teamdrhong.dto.publicTemplate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 공용 템플릿 조회를 위한 페이징 요청 DTO
 * 공용 템플릿에 특화된 검증 규칙을 포함
 */
@Getter
@Setter
@Schema(description = "공용 템플릿 페이징 요청 DTO")
public class PublicTemplatePageableRequest {
    
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private int page = 0;
    
    @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private int size = 10;
    
    @Schema(
        description = "정렬 필드", 
        example = "createdAt", 
        defaultValue = "createdAt",
        allowableValues = {"createdAt", "shareCount", "viewCount", "publicTemplateTitle"}
    )
    @NotNull(message = "정렬 필드는 필수입니다")
    @Pattern(
        regexp = "^(createdAt|shareCount|viewCount|publicTemplateTitle)$",
        message = "정렬 필드는 createdAt, shareCount, viewCount, publicTemplateTitle 중 하나여야 합니다"
    )
    private String sort = "createdAt";
    
    @Schema(description = "정렬 방향", example = "DESC", defaultValue = "DESC")
    @NotNull(message = "정렬 방향은 필수입니다")
    @Pattern(
        regexp = "^(ASC|DESC)$",
        message = "정렬 방향은 ASC 또는 DESC여야 합니다"
    )
    private String direction = "DESC";

    
    /**
     * 공용 템플릿용 Pageable 객체로 변환한다.
     * 
     * @return Pageable 객체
     */
    public Pageable toPageable() {
        Sort.Direction sortDirection = Sort.Direction.fromString(direction);
        return PageRequest.of(page, size, Sort.by(sortDirection, sort));
    }
}
