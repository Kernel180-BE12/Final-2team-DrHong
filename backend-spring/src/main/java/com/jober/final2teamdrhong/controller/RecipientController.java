package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.dto.recipient.RecipientRequest;
import com.jober.final2teamdrhong.dto.recipient.RecipientResponse;
import com.jober.final2teamdrhong.exception.ErrorResponse;
import com.jober.final2teamdrhong.service.RecipientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 수신자(Recipient) 관련 HTTP 요청을 처리하는 컨트롤러입니다.
 */
@Tag(name = "수신자 관리 API", description = "수신자 생성, 조회, 수정, 삭제 등 수신자와 관련된 API를 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/workspaces/{workspaceId}/recipients")
public class RecipientController {

    private final RecipientService recipientService;

    /**
     * 특정 워크스페이스에 새로운 수신자를 생성하는 API
     * <p>
     * 요청한 사용자가 해당 워크스페이스에 대한 접근 권한이 있는지 확인 후, 수신자를 생성합니다.
     * 만약 해당 워크스페이스에 동일한 이름과 전화번호를 가진 수신자가 이미 존재한다면 에러를 반환합니다.
     *
     * @param createDTO   클라이언트로부터 받은 수신자 생성을 위한 데이터 (JSON, @Valid로 검증됨)
     * @param workspaceId 수신자를 추가할 워크스페이스의 ID
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 201 (Created)와 함께 생성된 수신자의 정보를 담은 ResponseEntity
     */
    @Operation(summary = "수신자 생성", description = "특정 워크스페이스에 새로운 수신자를 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "수신자 생성 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RecipientResponse.SimpleDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 요청 데이터 유효성 검사 실패 또는 비즈니스 규칙 위배 (예: 권한 없는 워크스페이스, 이미 존재하는 수신자)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<RecipientResponse.SimpleDTO> createRecipient(@Valid @RequestBody RecipientRequest.CreateDTO createDTO,
                                                                       @PathVariable Integer workspaceId,
                                                                       @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        RecipientResponse.SimpleDTO createdRecipient = recipientService.createRecipient(createDTO, workspaceId, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdRecipient);
    }

    /**
     * 특정 워크스페이스에 속한 모든 수신자 목록을 페이징하여 조회하는 API
     * <p>
     * 요청한 사용자가 해당 워크스페이스에 대한 접근 권한이 있는지 확인 후, 수신자 목록을 반환합니다.
     *
     * @param workspaceId 수신자 목록을 조회할 워크스페이스의 ID
     * @param pageable    페이지 번호, 페이지 크기, 정렬 방법을 담은 객체
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 200 (OK)와 함께 페이징된 수신자 목록 정보를 담은 ResponseEntity
     */
    @Operation(summary = "수신자 목록 페이징 조회", description = "특정 워크스페이스에 속한 모든 수신자 목록을 페이징하여 조회합니다. " +
            "page, size, sort 파라미터를 사용하여 페이징을 제어할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수신자 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 존재하지 않거나 권한 없는 워크스페이스 접근",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<Page<RecipientResponse.SimpleDTO>> readRecipients(@PathVariable Integer workspaceId,
                                                                            @PageableDefault(size = 50,
                                                                                    sort = "createdAt",
                                                                                    direction = Sort.Direction.DESC)
                                                                            Pageable pageable,
                                                                            @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        Page<RecipientResponse.SimpleDTO> recipientPage = recipientService.readRecipients(workspaceId, currentUserId, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(recipientPage);
    }

    /**
     * 특정 워크스페이스에 속한 수신자 정보를 수정하는 API
     * <p>
     * 요청한 사용자가 해당 워크스페이스에 대한 접근 권한이 있는지 확인 후,
     * URL 경로의 recipientId에 해당하는 수신자 정보를 요청 본문의 데이터로 업데이트합니다.
     * 수정하려는 이름과 전화번호가 다른 수신자와 중복될 경우 에러를 반환합니다.
     *
     * @param updateDTO   수신자 수정을 위한 데이터 (JSON, @Valid로 검증됨)
     * @param workspaceId 수정할 수신자가 속한 워크스페이스의 ID
     * @param recipientId 수정할 수신자의 ID
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 200 (OK)와 함께 수정된 수신자의 정보를 담은 ResponseEntity
     */
    @Operation(summary = "수신자 정보 수정", description = "특정 수신자의 이름, 연락처, 메모를 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수신자 정보 수정 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RecipientResponse.SimpleDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 요청 데이터 유효성 검사 실패, 존재하지 않는 리소스(워크스페이스, 수신자), 또는 다른 수신자와 정보 중복",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{recipientId}")
    public ResponseEntity<RecipientResponse.SimpleDTO> updateRecipient(@Valid @RequestBody RecipientRequest.UpdateDTO updateDTO,
                                                                       @PathVariable Integer workspaceId,
                                                                       @PathVariable Integer recipientId,
                                                                       @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        RecipientResponse.SimpleDTO updatedRecipient = recipientService.updateRecipient(updateDTO, workspaceId, recipientId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(updatedRecipient);
    }

    /**
     * 특정 워크스페이스에 속한 수신자를 삭제하는 API (소프트 딜리트)
     * <p>
     * 요청한 사용자가 해당 워크스페이스에 대한 접근 권한이 있는지 확인 후,
     * URL 경로의 recipientId에 해당하는 수신자의 is_deleted 플래그를 true로 변경합니다.
     *
     * @param workspaceId 삭제할 수신자가 속한 워크스페이스의 ID
     * @param recipientId 삭제할 수신자의 ID
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 200 (OK)와 함께 삭제 처리된 수신자의 정보를 담은 ResponseEntity
     */
    @Operation(summary = "수신자 삭제", description = "특정 워크스페이스에 속한 수신자를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수신자 삭제 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = RecipientResponse.SimpleDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 존재하지 않거나 권한 없는 워크스페이스, " +
                    "또는 해당 워크스페이스에 속하지 않는 수신자에 접근 시 발생",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{recipientId}")
    public ResponseEntity<RecipientResponse.SimpleDTO> deleteRecipient(@PathVariable Integer workspaceId,
                                                                       @PathVariable Integer recipientId,
                                                                       @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        RecipientResponse.SimpleDTO deletedRecipient = recipientService.deleteRecipient(workspaceId, recipientId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(deletedRecipient);
    }
}
