package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.config.JwtConfig;
import com.jober.final2teamdrhong.dto.emailVerification.EmailRequest;
import com.jober.final2teamdrhong.dto.emailVerification.EmailVerificationResponse;
import com.jober.final2teamdrhong.dto.userLogin.TokenRefreshResponse;
import com.jober.final2teamdrhong.dto.userLogin.UserLoginRequest;
import com.jober.final2teamdrhong.dto.userLogin.UserLoginResponse;
import com.jober.final2teamdrhong.dto.userLogout.UserLogoutRequest;
import com.jober.final2teamdrhong.dto.userSignup.UserSignupRequest;
import com.jober.final2teamdrhong.dto.userSignup.UserSignupResponse;
import com.jober.final2teamdrhong.exception.ErrorResponse;
import com.jober.final2teamdrhong.service.AuthService;
import com.jober.final2teamdrhong.service.EmailService;
import com.jober.final2teamdrhong.service.RateLimitService;
import com.jober.final2teamdrhong.util.ClientIpUtil;
import com.jober.final2teamdrhong.util.LogMaskingUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Slf4j
@Tag(name = "인증", description = "사용자 인증 관련 API (회원가입, 로그인, 이메일 인증)")
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final JwtConfig jwtConfig;

    @Value("${app.environment.development:true}")
    private boolean isDevelopment;

    @Operation(summary = "이메일 인증 코드 발송", description = "회원가입을 위한 6자리 인증 코드를 이메일로 발송합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = EmailVerificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 이메일 형식 오류 등",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = EmailVerificationResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate Limit 초과",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = EmailVerificationResponse.class)))
    })
    @PostMapping("/send-verification-code")
    public ResponseEntity<EmailVerificationResponse> sendVerificationCode(
            @Parameter(description = "인증 코드를 받을 이메일 주소", required = true)
            @Valid @RequestBody EmailRequest emailRequest,
            HttpServletRequest request) {

        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);

        // Rate limiting 로직을 서비스로 위임
        emailService.sendVerificationCodeWithRateLimit(emailRequest.email(), clientIp);

        log.info("인증 코드 발송 성공: ip={}, email={}", clientIp, emailRequest.email());
        return ResponseEntity.ok(
            EmailVerificationResponse.success("인증 코드가 발송되었습니다.")
        );
    }

    @Operation(summary = "로컬 회원가입", description = "이메일 인증을 완료한 후 로컬 계정으로 회원가입을 진행합니다.")
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "회원가입 성공",
            content = @Content(
                schema = @Schema(implementation = UserSignupResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "회원가입 성공",
                    value = """
                    {
                        "success": true,
                        "message": "회원가입이 성공적으로 완료되었습니다.",
                        "data": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                schema = @Schema(implementation = UserSignupResponse.class),
                examples = {
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "중복 이메일",
                        value = """
                        {
                            "success": false,
                            "message": "이미 가입된 이메일입니다.",
                            "data": null
                        }
                        """
                    ),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "잘못된 인증코드",
                        value = """
                        {
                            "success": false,
                            "message": "인증 코드가 일치하지 않습니다.",
                            "data": null
                        }
                        """
                    ),
                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                        name = "유효성 검증 실패",
                        value = """
                        {
                            "success": false,
                            "message": "비밀번호는 6-20자의 대소문자, 숫자, 특수문자를 포함해야 합니다.",
                            "data": null
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "429",
            description = "Rate Limit 초과",
            content = @Content(
                schema = @Schema(implementation = UserSignupResponse.class),
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    name = "회원가입 속도 제한",
                    value = """
                    {
                        "success": false,
                        "message": "회원가입 속도 제한을 초과했습니다. 3600초 후 다시 시도해주세요.",
                        "data": null
                    }
                    """
                )
            ),
            headers = @io.swagger.v3.oas.annotations.headers.Header(
                name = "Retry-After",
                description = "다시 시도 가능한 시간(초)",
                schema = @Schema(type = "integer", example = "3600")
            )
        )
    })
    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponse> signup(
            @Parameter(description = "회원가입 요청 정보 (사용자명, 이메일, 비밀번호, 인증코드 포함)", required = true)
            @Valid @RequestBody UserSignupRequest userSignupRequest,
            HttpServletRequest request) {

        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);

        // Rate limiting과 회원가입 로직을 서비스로 위임
        authService.signupWithRateLimit(userSignupRequest, clientIp);

        log.info("회원가입 성공: ip={}, email={}", clientIp, userSignupRequest.email());
        return ResponseEntity.ok(
            UserSignupResponse.success("회원가입이 성공적으로 완료되었습니다.")
        );
    }

    @Operation(summary = "로컬 로그인", description = "이메일과 비밀번호를 사용하여 로컬 계정으로 로그인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserLoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청: 인증 실패 등",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserLoginResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate Limit 초과",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = UserLoginResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(
            @Parameter(description = "로그인 요청 정보 (이메일, 비밀번호)", required = true)
            @RequestBody @Valid UserLoginRequest userLoginRequest,
            HttpServletRequest request) {

        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);

        // 향상된 Rate limiting 체크 (IP + 이메일 기반)
        rateLimitService.checkEnhancedLoginRateLimit(clientIp, userLoginRequest.email());

        UserLoginResponse response = authService.loginWithRefreshToken(userLoginRequest, clientIp);

        // 보안 강화: 민감한 정보 마스킹 후 로깅
        log.info("로그인 API 완료: ip={}, email={}",
                LogMaskingUtil.maskIpAddress(clientIp),
                LogMaskingUtil.maskEmail(userLoginRequest.email()));

        // 보안 개선: Authorization 헤더에는 토큰을 포함하지 않고, 응답 바디에만 포함
        // 클라이언트는 응답 바디에서 토큰을 추출하여 이후 요청의 헤더에 포함해야 함
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "토큰 갱신", description = "만료된 Access Token을 Refresh Token으로 갱신합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = TokenRefreshResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate Limit 초과",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Parameter(description = "Authorization 헤더 (Bearer + Refresh Token)", required = true)
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest request) {

        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);

        // Rate limiting 체크 (토큰 갱신 남용 방지)
        rateLimitService.checkRefreshTokenRateLimit(clientIp);

        // AuthService에 토큰 갱신 처리 위임
        TokenRefreshResponse response = authService.refreshTokens(authorizationHeader, clientIp);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "현재 세션을 종료하고 모든 토큰을 무효화합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Parameter(description = "Authorization 헤더 (Bearer + Access Token)")
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Parameter(description = "로그아웃 요청 (Refresh Token 포함)")
            @Valid @RequestBody UserLogoutRequest logoutRequest,
            HttpServletRequest request) {

        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);

        // AuthService에 로그아웃 처리 위임
        String accessToken = jwtConfig.extractTokenFromHeader(authorizationHeader);
        authService.logout(accessToken, logoutRequest.refreshToken(), clientIp);

        return ResponseEntity.noContent().build();
    }
}