package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.dto.workspace.WorkspaceRequest;
import com.jober.final2teamdrhong.dto.workspace.WorkspaceResponse;
import com.jober.final2teamdrhong.exception.ErrorResponse;
import com.jober.final2teamdrhong.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 워크스페이스 관련 HTTP 요청을 처리하는 컨트롤러입니다.
 */
@Tag(name = "워크스페이스 관리 API", description = "워크스페이스 생성, 조회, 수정, 삭제 등 워크스페이스와 관련된 API를 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    /**
     * 새로운 워크스페이스를 생성하는 API
     * <p>
     * 요청 본문(RequestBody)으로 받은 데이터를 사용하여 워크스페이스를 생성하며, 이 API를 호출하기 위해서는 인증이 필요합니다.
     *
     * @param createDTO 클라이언트로부터 받은 워크스페이스 생성을 위한 데이터 전송 객체 (DTO) (JSON, @Valid로 검증됨)
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 201 (Created)와 함께 생성된 워크스페이스의 간략 정보를 담은 ResponseEntity
     */
    @Operation(summary = "워크스페이스 생성", description = "새로운 워크스페이스를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "워크스페이스 생성 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceResponse.SimpleDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 요청 데이터 유효성 검사에 실패했거나, 비즈니스 규칙에 위배됩니다. (예: 존재하지 않는 사용자, 중복된 URL)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<WorkspaceResponse.SimpleDTO> createWorkspace(@Valid @RequestBody WorkspaceRequest.CreateDTO createDTO,
                                                                       @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        WorkspaceResponse.SimpleDTO createdWorkspace = workspaceService.createWorkspace(createDTO, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdWorkspace);
    }

    /**
     * 현재 인증된 사용자가 속한 모든 워크스페이스 목록을 조회합니다.
     * <p>
     * API 호출 시 전달된 JWT 토큰의 인증 정보를 기반으로, 해당 사용자가 접근 가능한 워크스페이스의 간략한 정보 목록을 반환합니다.
     *
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 200 (OK)와 함께 조회된 워크스페이스 간략 정보(SimpleDTO) 리스트를 담은 ResponseEntity
     */
    @Operation(summary = "워크스페이스 목록 조회", description = "현재 로그인된 사용자가 접근 가능한 모든 워크스페이스의 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "워크스페이스 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = WorkspaceResponse.SimpleDTO.class)))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 해당 사용자를 찾을 수 없음",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<WorkspaceResponse.SimpleDTO>> readWorkspaces(@AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        List<WorkspaceResponse.SimpleDTO> workspaceList = workspaceService.readWorkspaces(currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(workspaceList);
    }

    /**
     * 특정 워크스페이스의 상세 정보를 조회하는 API
     * <p>
     * 경로 변수(PathVariable)로 받은 ID를 사용하여 특정 워크스페이스의 상세 정보를 조회합니다.
     * 요청한 사용자가 해당 워크스페이스에 접근 권한이 있는지 확인하는 인가 과정이 포함됩니다.
     *
     * @param workspaceId 조회할 워크스페이스의 ID
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 200 (OK)와 함께 조회된 워크스페이스 상세 정보(DetailDTO)를 담은 ResponseEntity
     */
    @Operation(summary = "워크스페이스 상세 조회", description = "특정 워크스페이스의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "워크스페이스 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceResponse.DetailDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 워크스페이스를 찾을 수 없거나 접근 권한이 없음",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse.DetailDTO> readWorkspaceDetail(@PathVariable Integer workspaceId,
                                                                           @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        WorkspaceResponse.DetailDTO workspaceDetail = workspaceService.readWorkspaceDetail(workspaceId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(workspaceDetail);
    }

    /**
     * 특정 워크스페이스의 정보를 수정하는 API
     * <p>
     * 경로 변수(PathVariable)로 받은 워크스페이스 ID와 요청 본문(RequestBody)의 수정 데이터를 사용하여
     * 특정 워크스페이스의 정보를 업데이트합니다.
     * 요청한 사용자가 해당 워크스페이스에 접근 권한이 있는지 확인하는 인가 과정이 포함됩니다.
     *
     * @param updateDTO   워크스페이스 수정을 위한 요청 데이터 (JSON, @Valid로 검증됨)
     * @param workspaceId 수정할 워크스페이스의 ID
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 200 (OK)와 함께 수정된 워크스페이스 상세 정보(DetailDTO)를 담은 ResponseEntity
     */
    @Operation(summary = "워크스페이스 정보 수정", description = "특정 워크스페이스의 상세 정보를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "워크스페이스 정보 수정 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceResponse.DetailDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 요청 데이터 유효성 검사에 실패했거나, 비즈니스 규칙에 위배됩니다. (예: 워크스페이스를 찾을 수 없음, 접근 권한 없음, 중복된 URL)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse.DetailDTO> updateWorkspace(@Valid @RequestBody WorkspaceRequest.UpdateDTO updateDTO,
                                                                       @PathVariable Integer workspaceId,
                                                                       @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        WorkspaceResponse.DetailDTO updatedWorkspace = workspaceService.updateWorkspace(updateDTO, workspaceId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(updatedWorkspace);
    }

    /**
     * 특정 워크스페이스를 삭제하는 API (소프트 딜리트)
     * <p>
     * 요청한 사용자가 해당 워크스페이스의 소유자인지 확인하는 인가 과정이 포함되며,
     * 실제 데이터는 삭제되지 않고 삭제 플래그(is_deleted)만 변경됩니다.
     *
     * @param workspaceId 삭제할 워크스페이스의 ID
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 200 (OK)와 함께 삭제된 워크스페이스의 간략 정보를 담은 ResponseEntity
     */
    @Operation(summary = "워크스페이스 삭제", description = "특정 워크스페이스를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "워크스페이스 삭제 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = WorkspaceResponse.SimpleDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 워크스페이스를 찾을 수 없거나 접근 권한이 없습니다.",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{workspaceId}")
    public ResponseEntity<WorkspaceResponse.SimpleDTO> deleteWorkspace(@PathVariable Integer workspaceId,
                                                                       @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        WorkspaceResponse.SimpleDTO deletedWorkspace = workspaceService.deleteWorkspace(workspaceId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(deletedWorkspace);
    }
}
