package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplatePageableRequest;
import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplateResponse;
import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.service.IndividualTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;



@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "개인 템플릿 API", description = "개인 템플릿 생성/조회/수정/삭제 관련 API")

public class IndividualTemplateController {

    private final IndividualTemplateService individualTemplateService;

    /**
     * 동기 빈 템플릿 생성 API
     * body: {"workspaceId" : 123}
     */
    @PostMapping("/templates/{workspaceId}")
    @Operation(
            summary = "빈 템플릿 생성",
            description = "Workspace ID를 기반으로 빈 템플릿을 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "템플릿 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류 발생")
    })
    public ResponseEntity<IndividualTemplateResponse> createEmptyTemplate(
            @Parameter(description = "Workspace ID", example = "1")
            @PathVariable Integer workspaceId,
            @AuthenticationPrincipal JwtClaims claims
    ) {
        Integer userId = claims.getUserId();
        individualTemplateService.validateWorkspaceOwnership(workspaceId, userId);

        IndividualTemplateResponse response = individualTemplateService.createTemplate(workspaceId);
        return ResponseEntity.ok(response);
    }

    /**
     * 비동기 빈 템플릿 생성 API
     */
    @PostMapping("/templates/{workspaceId}/async")
    @Operation(
            summary = "빈 템플릿 생성(비동기 @Async)",
            description = "Workspace ID를 기반으로 빈 템플릿을 비동기로 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "요청 접수됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "500", description = "서버 오류 발생")
    })
    public ResponseEntity<IndividualTemplateResponse> createEmptyTemplateAsync(
            @Parameter(description = "Workspace ID", example = "1")
            @PathVariable Integer workspaceId,
            @AuthenticationPrincipal JwtClaims claims) {

        Integer userId = claims.getUserId();
        individualTemplateService.validateWorkspaceOwnership(workspaceId, userId);

        // 비동기 호출 (여기서는 join()으로 결과를 가져옴 → 403 안나옴)
        IndividualTemplateResponse response =
                individualTemplateService.createTemplateAsync(workspaceId).join();

        return ResponseEntity.status(200).body(response);
    }

    /**
     * 동기 공용 템플릿 기반 개인 템플릿 생성
     */
    @PostMapping("/templates/{workspaceId}/from-public/{publicTemplateId}")
    @Operation(
            summary = "공용 템플릿 기반 개인 템플릿 생성",
            description = "공용 템플릿의 내용을 복사하여 지정한 워크스페이스에 개인 템플릿 생성",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "개인 템플릿 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "워크스페이스 접근 권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류 발생")
    })
    public ResponseEntity<IndividualTemplateResponse> createFromPublicTemplate(
            @Parameter(description = "공용 템플릿 ID", example = "5")
            @PathVariable Integer publicTemplateId,
            @Parameter(description = "개인 템플릿을 생성할 워크스페이스 ID", example = "1")
            @PathVariable Integer workspaceId,
            @AuthenticationPrincipal JwtClaims claims
    ) {
        Integer userId = claims.getUserId();
        individualTemplateService.validateWorkspaceOwnership(workspaceId,userId);

        IndividualTemplateResponse response =
                individualTemplateService.createIndividualTemplateFromPublic(publicTemplateId, workspaceId);

        return ResponseEntity.ok(response);
    }

    /**
     * 공용 템플릿 기반 개인 템플릿 생성 (비동기)
     */
    @PostMapping("/templates/{workspaceId}/from-public/{publicTemplateId}/async")
    @Operation(
            summary = "공용 템플릿 기반 개인 템플릿 생성(비동기 @Async)",
            description = "공용 템플릿의 내용을 복사하여 지정한 워크스페이스에 개인 템플릿을 비동기로 생성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "요청 접수됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "403", description = "워크스페이스 접근 권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류 발생")
    })
    public ResponseEntity<IndividualTemplateResponse> createFromPublicTemplateAsync(
            @Parameter(description = "공용 템플릿 ID", example = "5")
            @PathVariable Integer publicTemplateId,
            @Parameter(description = "개인 템플릿을 생성할 워크스페이스 ID", example = "1")
            @PathVariable Integer workspaceId,
            @AuthenticationPrincipal JwtClaims claims
    ) {
        Integer userId = claims.getUserId();
        individualTemplateService.validateWorkspaceOwnership(workspaceId, userId);

        // 비동기 호출 후 join()으로 결과 가져오기 → 403 방지
        IndividualTemplateResponse response =
                individualTemplateService.createIndividualTemplateFromPublicAsync(publicTemplateId, workspaceId).join();
        return ResponseEntity.status(200).body(response);
    }

    // 전체 조회 (동기)
    @Operation(summary = "워크스페이스 별 개인 템플릿 목록 전체 조회", description = "페이지네이션 및 정렬 조건 지원")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "개별 템플릿 조회 성공") })
    @GetMapping("/{workspaceId}/templates")
    public ResponseEntity<Page<IndividualTemplateResponse>> getAllTemplates(
            @Parameter(description = "워크스페이스 ID", example = "1")
            @PathVariable Integer workspaceId,
            @Valid @ParameterObject IndividualTemplatePageableRequest individualTemplatePageableRequest,
            @AuthenticationPrincipal JwtClaims claims) {
        Integer userId = claims.getUserId();
        individualTemplateService.validateWorkspaceOwnership(workspaceId, userId);

        Page<IndividualTemplateResponse> page = individualTemplateService.getAllTemplates(
                workspaceId,
                individualTemplatePageableRequest);
        return ResponseEntity.ok(page);
    }

    // 전체 조회 (비동기)
    @Operation(summary = "워크스페이스 별 템플릿 목록 전체 조회 (비동기 @Async)")
    @GetMapping("/{workspaceId}/templates/async")
    public ResponseEntity<Page<IndividualTemplateResponse>> getAllTemplatesAsync(
            @Parameter(description = "워크스페이스 ID", example = "1")
            @PathVariable Integer workspaceId,
            @Valid @ParameterObject IndividualTemplatePageableRequest individualTemplatePageableRequest,
            @AuthenticationPrincipal JwtClaims claims) {
        Integer userId = claims.getUserId();
        individualTemplateService.validateWorkspaceOwnership(workspaceId, userId);

        Page<IndividualTemplateResponse> page = individualTemplateService.getAllTemplatesAsync(
                workspaceId,
                individualTemplatePageableRequest
        ).join();
        return ResponseEntity.status(200).body(page);
    }

    // 단일 조회 (동기)
    @Operation(summary = "템플릿 단일 조회", description = "워크스페이스 별 개인 템플릿을 단일 조회")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "템플릿 없음")
    })
    @GetMapping("/{workspaceId}/templates/{individualTemplateId}")
    public ResponseEntity<IndividualTemplateResponse> getTemplate(
            @Parameter(description = "워크스페이스 ID", example = "1")
            @PathVariable Integer workspaceId,
            @Parameter(description = "개인 템플릿 ID", example = "2")
            @PathVariable Integer individualTemplateId,
            @AuthenticationPrincipal JwtClaims claims) {
        Integer userId = claims.getUserId();
        individualTemplateService.validateWorkspaceOwnership(workspaceId, userId);

        return ResponseEntity.ok(individualTemplateService.getIndividualTemplate(workspaceId, individualTemplateId));
    }

    // 단일 조회 (비동기)
    @Operation(summary = "템플릿 단일 조회 (비동기 @Async)")
    @GetMapping("/{workspaceId}/templates/{individualTemplateId}/async")
    public ResponseEntity<IndividualTemplateResponse> getTemplateAsync(
            @Parameter(description = "워크스페이스 ID", example = "1")
            @PathVariable Integer workspaceId,
            @Parameter(description = "개인 템플릿 ID", example = "2")
            @PathVariable Integer individualTemplateId,
            @AuthenticationPrincipal JwtClaims claims) {
        Integer userId = claims.getUserId();
        individualTemplateService.validateWorkspaceOwnership(workspaceId, userId);

        // 비동기 실행 후 join()으로 결과 획득
        IndividualTemplateResponse response = individualTemplateService
                .getIndividualTemplateAsync(workspaceId, individualTemplateId)
                .join();

        return ResponseEntity.status(200).body(response);
    }
}
