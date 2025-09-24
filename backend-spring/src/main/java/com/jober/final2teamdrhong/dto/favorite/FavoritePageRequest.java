package com.jober.final2teamdrhong.dto.favorite;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoritePageRequest {

    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
    @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다")
    private int page = 0;

    @Schema(description = "페이지 크기", example = "10", defaultValue = "10")
    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다")
    private int size = 10;
}