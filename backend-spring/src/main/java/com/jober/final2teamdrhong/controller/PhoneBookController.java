package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.dto.phonebook.PhoneBookRequest;
import com.jober.final2teamdrhong.dto.phonebook.PhoneBookResponse;
import com.jober.final2teamdrhong.exception.ErrorResponse;
import com.jober.final2teamdrhong.service.PhoneBookService;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * 주소록(PhoneBook) 관련 HTTP 요청을 처리하는 컨트롤러입니다.
 */
@Tag(name = "주소록 관리 API", description = "주소록 생성, 조회, 수정, 삭제 등 주소록과 관련된 API를 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("workspaces/{workspaceId}/phonebooks")
public class PhoneBookController {

    private final PhoneBookService phoneBookService;

    /**
     * 특정 워크스페이스에 새로운 주소록을 생성하는 API
     * <p>
     * 요청한 사용자가 해당 워크스페이스에 대한 접근 권한이 있는지 확인 후, 주소록을 생성합니다.
     *
     * @param createDTO   클라이언트로부터 받은 주소록 생성을 위한 데이터 (JSON, @Valid로 검증됨)
     * @param workspaceId 주소록을 추가할 워크스페이스의 ID
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 201 (Created)와 함께 생성된 주소록의 정보를 담은 ResponseEntity
     */
    @Operation(summary = "주소록 생성", description = "특정 워크스페이스에 새로운 주소록을 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "주소록 생성 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PhoneBookResponse.SimpleDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 요청 데이터 유효성 검사 실패 또는 비즈니스 규칙 위배 (예: 권한 없는 워크스페이스)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<PhoneBookResponse.SimpleDTO> createPhoneBook(@Valid @RequestBody PhoneBookRequest.CreateDTO createDTO,
                                                                       @PathVariable Integer workspaceId,
                                                                       @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        PhoneBookResponse.SimpleDTO createdPhoneBook = phoneBookService.createPhoneBook(createDTO, workspaceId, currentUserId);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdPhoneBook);
    }

    /**
     * 특정 주소록에 다수의 수신자를 추가하는 API
     * <p>
     * 요청된 수신자 ID 목록을 받아, 해당 주소록에 일괄적으로 추가합니다.
     * 이미 주소록에 포함된 수신자는 중복으로 추가되지 않으며, 최종적으로 실제로 추가된 수신자 목록만 반환합니다.
     *
     * @param recipientIdListDTO 클라이언트로부터 받은 추가할 수신자 ID 목록을 담은 DTO
     * @param workspaceId      주소록이 속한 워크스페이스의 ID
     * @param phoneBookId      수신자를 추가할 주소록의 ID
     * @param jwtClaims {@link AuthenticationPrincipal}을 통해 SecurityContext에서 직접 주입받는 현재 로그인된 사용자의 JWT 정보 객체
     * @return 상태 코드 200 (OK)와 함께, 실제로 추가된 수신자 정보를 담은 ResponseEntity
     */
    @Operation(summary = "주소록에 수신자 일괄 추가", description = "특정 주소록에 한 명 이상의 수신자를 일괄 추가합니다. 요청된 수신자 중 이미 추가된 멤버는 자동으로 제외됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수신자 추가 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PhoneBookResponse.ModifiedRecipientsDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 요청 데이터 유효성 검사 실패, 존재하지 않는 ID(워크스페이스, 주소록, 수신자) 포함, 또는 기타 비즈니스 규칙 위배",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{phoneBookId}/recipients")
    public ResponseEntity<PhoneBookResponse.ModifiedRecipientsDTO> addRecipientsToPhoneBook(@RequestBody PhoneBookRequest.RecipientIdListDTO recipientIdListDTO,
                                                                                            @PathVariable Integer workspaceId,
                                                                                            @PathVariable Integer phoneBookId,
                                                                                            @AuthenticationPrincipal JwtClaims jwtClaims) {
        Integer currentUserId = jwtClaims.getUserId();
        PhoneBookResponse.ModifiedRecipientsDTO addedRecipients = phoneBookService.addRecipientsToPhoneBook(recipientIdListDTO, workspaceId, phoneBookId, currentUserId);

        return ResponseEntity.status(HttpStatus.OK).body(addedRecipients);
    }
}
