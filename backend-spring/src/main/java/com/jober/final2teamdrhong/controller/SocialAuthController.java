package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.config.AuthProperties;
import com.jober.final2teamdrhong.dto.auth.OAuth2TempUserInfo;
import com.jober.final2teamdrhong.dto.auth.SocialSignupRequest;
import com.jober.final2teamdrhong.dto.auth.SocialSignupResponse;
import com.jober.final2teamdrhong.service.AuthService;
import com.jober.final2teamdrhong.service.OAuth2TempStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 소셜 로그인 관련 API를 제공하는 컨트롤러
 * OAuth2 소셜 로그인 후 회원가입 완료, 임시 정보 조회 등의 기능을 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/auth/social")
@RequiredArgsConstructor
@Tag(name = "소셜 인증", description = "OAuth2 소셜 로그인 관련 API")
public class SocialAuthController {

    private final AuthService authService;
    private final OAuth2TempStorageService oAuth2TempStorageService;
    private final AuthProperties authProperties;

    /**
     * 소셜 로그인 시작 (구글)
     * 클라이언트를 Google OAuth2 인증 페이지로 리다이렉트합니다.
     *
     * @return 구글 OAuth2 인증 URL로 리다이렉트
     */
    @GetMapping("/login/google")
    @Operation(
            summary = "구글 소셜 로그인 시작",
            description = "구글 OAuth2 인증 페이지로 리다이렉트하여 소셜 로그인을 시작합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "구글 OAuth2 인증 페이지로 리다이렉트"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<Void> loginWithGoogle() {
        // Spring Security OAuth2가 자동으로 처리하는 URL로 리다이렉트
        // context-path를 포함한 전체 경로로 리다이렉트
        return ResponseEntity.status(302)
                .header("Location", "/api/oauth2/authorization/google")
                .build();
    }

    /**
     * 소셜 로그인 지원 제공자 목록 조회
     * 현재 지원하는 OAuth2 제공자 목록을 반환합니다.
     *
     * @return 지원하는 소셜 로그인 제공자 목록
     */
    @GetMapping("/providers")
    @Operation(
            summary = "지원하는 소셜 로그인 제공자 목록",
            description = "현재 애플리케이션에서 지원하는 OAuth2 소셜 로그인 제공자 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "제공자 목록 조회 성공"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> getSupportedProviders() {
        var providers = java.util.Map.of(
                "google", java.util.Map.of(
                        "name", "Google",
                        "loginUrl", "/api/auth/social/login/google",
                        "fullUrl", "http://localhost:8080/api/auth/social/login/google",
                        "icon", "https://developers.google.com/identity/images/g-logo.png",
                        "note", "Swagger에서는 CORS로 인해 작동하지 않습니다. 브라우저에서 fullUrl을 직접 열어주세요."
                )
                // 향후 네이버, 카카오 등 추가 예정
        );

        return ResponseEntity.ok(java.util.Map.of(
                "providers", providers,
                "message", "현재 지원하는 소셜 로그인 제공자 목록입니다. OAuth2 로그인은 브라우저에서 직접 실행해주세요."
        ));
    }

    /**
     * 소셜 로그인 회원가입 완료
     * OAuth2로 받은 임시 정보와 사용자가 입력한 핸드폰 번호를 결합하여 회원가입을 완료합니다.
     *
     * @param request 소셜 회원가입 요청 정보
     * @return 회원가입 완료 응답 (JWT 토큰 포함)
     */
    @PostMapping("/signup")
    @Operation(
            summary = "소셜 로그인 회원가입 완료",
            description = "OAuth2 임시 정보와 핸드폰 번호를 결합하여 회원가입을 완료하고 JWT 토큰을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 임시 키, 인증 코드 불일치 등)"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 사용자"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<SocialSignupResponse> completeSocialSignup(
            @Valid @RequestBody SocialSignupRequest request) {

        log.info("소셜 회원가입 완료 요청 - 임시 키: {}, 핸드폰: {}",
                request.tempKey(), request.getNormalizedPhoneNumber());

        // AuthService를 통해 소셜 회원가입 처리
        // BusinessException이 발생하면 GlobalExceptionHandler가 처리
        SocialSignupResponse response = authService.completeSocialSignup(request);

        log.info("소셜 회원가입 완료 성공 - 사용자 ID: {}, 이메일: {}",
                response.userId(), response.email());

        return ResponseEntity.ok(response);
    }

    /**
     * OAuth2 임시 사용자 정보 조회
     * 프론트엔드에서 핸드폰 번호 입력 페이지 로딩 시 사용자 정보를 미리 표시하기 위해 사용합니다.
     *
     * @param tempKey OAuth2 임시 정보 키
     * @return 임시 사용자 정보
     */
    @GetMapping("/temp-info")
    @Operation(
            summary = "OAuth2 임시 사용자 정보 조회",
            description = "임시 키를 사용하여 OAuth2로 받은 사용자 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "임시 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 임시 키"),
            @ApiResponse(responseCode = "404", description = "임시 정보를 찾을 수 없음 (만료되었거나 존재하지 않음)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> getTempUserInfo(
            @Parameter(description = "OAuth2 임시 정보 키", required = true)
            @RequestParam String tempKey) {

        log.info("OAuth2 임시 사용자 정보 조회 요청 - 키: {}", tempKey);

        // 임시 사용자 정보 조회
        // BusinessException이 발생하면 GlobalExceptionHandler가 처리
        OAuth2TempUserInfo tempUserInfo = oAuth2TempStorageService.retrieveTempUserInfo(tempKey);

        if (tempUserInfo == null) {
            log.warn("OAuth2 임시 사용자 정보를 찾을 수 없음 - 키: {}", tempKey);
            return ResponseEntity.notFound().build();
        }

        // 민감한 정보는 제외하고 필요한 정보만 반환
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("email", tempUserInfo.email());
        responseData.put("name", tempUserInfo.name());
        responseData.put("provider", tempUserInfo.provider());
        responseData.put("imageUrl", tempUserInfo.imageUrl());

        // TTL 정보도 함께 제공 (프론트엔드에서 만료 시간 표시용)
        long ttlSeconds = oAuth2TempStorageService.getTempUserInfoTTL(tempKey);
        if (ttlSeconds > 0) {
            responseData.put("expiresInSeconds", ttlSeconds);
        }

        log.info("OAuth2 임시 사용자 정보 조회 성공 - 이메일: {}, 제공자: {}",
                tempUserInfo.email(), tempUserInfo.provider());

        return ResponseEntity.ok(responseData);
    }

    /**
     * OAuth2 임시 정보 만료 시간 연장
     * 사용자가 핸드폰 인증 과정에서 시간이 더 필요한 경우 사용합니다.
     *
     * @param tempKey 연장할 임시 키
     * @param additionalMinutes 추가할 분 수 (기본값: 10분)
     * @return 연장 결과
     */
    @PostMapping("/extend-temp-info")
    @Operation(
            summary = "OAuth2 임시 정보 만료 시간 연장",
            description = "임시 정보의 만료 시간을 연장합니다. (핸드폰 인증 시간이 부족한 경우 사용)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "만료 시간 연장 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 임시 키"),
            @ApiResponse(responseCode = "404", description = "임시 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<?> extendTempInfo(
            @Parameter(description = "OAuth2 임시 정보 키", required = true)
            @RequestParam String tempKey,
            @Parameter(description = "추가할 분 수 (기본값: 10분)")
            @RequestParam(defaultValue = "10") int additionalMinutes) {

        log.info("OAuth2 임시 정보 만료 시간 연장 요청 - 키: {}, 추가 시간: {}분", tempKey, additionalMinutes);

        // 만료 시간 연장 (최대 제한 적용)
        int maxExtensionMinutes = authProperties.getOauth2().getMaxExtensionMinutes();
        int limitedMinutes = Math.min(additionalMinutes, maxExtensionMinutes);
        boolean extended = oAuth2TempStorageService.extendTempUserInfo(tempKey, limitedMinutes);

        if (!extended) {
            log.warn("OAuth2 임시 정보 만료 시간 연장 실패 - 키: {}", tempKey);
            return ResponseEntity.notFound()
                    .build();
        }

        // 연장 후 새로운 TTL 조회
        long newTtlSeconds = oAuth2TempStorageService.getTempUserInfoTTL(tempKey);

        var responseData = java.util.Map.of(
                "message", "만료 시간이 연장되었습니다.",
                "extendedMinutes", limitedMinutes,
                "newExpiresInSeconds", newTtlSeconds
        );

        log.info("OAuth2 임시 정보 만료 시간 연장 성공 - 키: {}, 새 TTL: {}초", tempKey, newTtlSeconds);

        return ResponseEntity.ok(responseData);
    }
}