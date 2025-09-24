package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.dto.favorite.FavoritePageRequest;
import com.jober.final2teamdrhong.dto.favorite.FavoriteResponse;
import com.jober.final2teamdrhong.dto.favorite.IndividualTemplateFavoriteRequest;
import com.jober.final2teamdrhong.dto.favorite.PublicTemplateFavoriteRequest;
import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.exception.ErrorResponse;
import com.jober.final2teamdrhong.entity.Favorite.TemplateType;
import com.jober.final2teamdrhong.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "즐겨찾기 API", description = "즐겨찾기 생성/조회/삭제 관련 API")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "개인 템플릿 즐겨찾기 추가", description = "사용자가 개인 템플릿을 즐겨찾기에 추가합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "개인 템플릿 즐겨찾기 추가 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FavoriteResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/individual/favorite")
    public ResponseEntity<FavoriteResponse> createIndividualTemplateFavorite(
            @AuthenticationPrincipal JwtClaims jwtClaims,
            @Valid @RequestBody IndividualTemplateFavoriteRequest request) {
        FavoriteResponse response = favoriteService.createIndividualTemplateFavorite(jwtClaims, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }



    @Operation(summary = "공용 템플릿 즐겨찾기 추가", description = "사용자가 공용 템플릿을 즐겨찾기에 추가합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "공용 템플릿 즐겨찾기 추가 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = FavoriteResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/public/favorite")
    public ResponseEntity<FavoriteResponse> createPublicTemplateFavorite(
            @AuthenticationPrincipal JwtClaims jwtClaims,
            @Valid @RequestBody PublicTemplateFavoriteRequest request) {
        FavoriteResponse response = favoriteService.createPublicTemplateFavorite(jwtClaims, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }



    @Operation(summary = "워크스페이스별 즐겨찾기 목록 조회", description = "특정 워크스페이스에 속한 즐겨찾기 목록을 조건에 따라 페이징하여 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "즐겨찾기 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음 (워크스페이스가 존재하지 않거나, 사용자에게 권한이 없음)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/workspace/{workspaceId}/favorites")
    public ResponseEntity<Page<FavoriteResponse>> getFavoritesByWorkspace(
            @AuthenticationPrincipal JwtClaims jwtClaims,
            @PathVariable("workspaceId") Integer workspaceId,
            @RequestParam(value = "templateType", required = false) TemplateType templateType,
            @Valid @ParameterObject FavoritePageRequest favoritePageRequest) {

        Page<FavoriteResponse> favorites = favoriteService.getFavoritesByWorkspace(jwtClaims, workspaceId, templateType, favoritePageRequest);
        return ResponseEntity.ok(favorites);
    }
}
