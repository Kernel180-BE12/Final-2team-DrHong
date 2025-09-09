package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.dto.EmailRequest;
import com.jober.final2teamdrhong.dto.UserLoginRequest;
import com.jober.final2teamdrhong.dto.UserLoginResponse;
import com.jober.final2teamdrhong.dto.UserSignupRequest;
import com.jober.final2teamdrhong.dto.UserSignupResponse;
import com.jober.final2teamdrhong.service.EmailService;
import com.jober.final2teamdrhong.service.RateLimitService;
import com.jober.final2teamdrhong.service.RefreshTokenService;
import com.jober.final2teamdrhong.service.UserService;
import com.jober.final2teamdrhong.service.BlacklistService;
import com.jober.final2teamdrhong.config.JwtConfig;
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
@RequestMapping("/api/auth")
@Slf4j
@Tag(name = "인증", description = "사용자 인증 관련 API (회원가입, 이메일 인증)")
public class UserController {

    private final UserService userService;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final RefreshTokenService refreshTokenService;
    private final BlacklistService blacklistService;
    private final JwtConfig jwtConfig;


    @Value("${app.environment.development:true}")
    private boolean isDevelopment;

    @Operation(
            summary = "이메일 인증 코드 발송",
            description = """
                    ## 📧 이메일 인증 코드 발송
                    
                    회원가입을 위한 6자리 인증 코드를 이메일로 발송합니다.
                    
                    ### ⚡ Rate Limiting
                    - **제한**: IP당 5분간 3회
                    - **초과 시**: HTTP 429 상태코드와 Retry-After 헤더 반환
                    
                    ### 🔧 개발환경 정보
                    - 실제 이메일 발송 비활성화
                    - 인증 코드는 로그에서 확인 가능
                    - Redis 대신 인메모리 저장소 사용
                    
                    ### 📝 사용법
                    1. 유효한 이메일 주소 입력
                    2. 발송된 인증 코드 확인 (개발환경: 로그 확인)
                    3. 회원가입 API에서 인증 코드 사용
                    """,
            tags = {"인증"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ 인증 코드 발송 성공",
                    content = @Content(
                            schema = @Schema(implementation = UserSignupResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "성공 응답",
                                    value = """
                                            {
                                                "success": true,
                                                "message": "인증 코드가 발송되었습니다.",
                                                "data": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ 잘못된 요청 (이메일 형식 오류 등)",
                    content = @Content(
                            schema = @Schema(implementation = UserSignupResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "이메일 형식 오류",
                                    value = """
                                            {
                                                "success": false,
                                                "message": "올바른 이메일 형식이 아닙니다.",
                                                "data": null
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "🚫 Rate Limit 초과",
                    content = @Content(
                            schema = @Schema(implementation = UserSignupResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "속도 제한 초과",
                                    value = """
                                            {
                                                "success": false,
                                                "message": "이메일 발송 속도 제한을 초과했습니다. 180초 후 다시 시도해주세요.",
                                                "data": null
                                            }
                                            """
                            )
                    ),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "Retry-After",
                            description = "다시 시도 가능한 시간(초)",
                            schema = @Schema(type = "integer", example = "180")
                    )
            )
    })
    @PostMapping("/send-verification-code")
    public ResponseEntity<UserSignupResponse> sendVerificationCode(
            @Parameter(description = "인증 코드를 받을 이메일 주소", required = true)
            @Valid @RequestBody EmailRequest emailRequestDto,
            HttpServletRequest request) {

        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);

        // Rate limiting 로직을 서비스로 위임
        emailService.sendVerificationCodeWithRateLimit(emailRequestDto.getEmail(), clientIp);

        log.info("인증 코드 발송 성공: ip={}, email={}", clientIp, emailRequestDto.getEmail());
        return ResponseEntity.ok(
                UserSignupResponse.success("인증 코드가 발송되었습니다.")
        );
    }

    @Operation(
            summary = "로컬 회원가입",
            description = """
                    ## 👤 로컬 계정 회원가입
                    
                    이메일 인증을 완료한 후 로컬 계정으로 회원가입을 진행합니다.
                    
                    ### 📋 필수 조건
                    1. **이메일 인증 완료**: 먼저 `/send-verification-code`로 인증 코드 발송
                    2. **유효한 인증 코드**: 발송된 6자리 인증 코드 입력
                    3. **유효성 검증 통과**: 모든 필드가 검증 규칙을 만족해야 함
                    
                    ### 🔒 보안 기능
                    - **비밀번호 암호화**: BCrypt 해싱
                    - **타이밍 공격 방지**: 상수시간 인증 코드 비교
                    - **중복 가입 방지**: 이메일 중복 검사
                    - **Rate Limiting**: IP당 1시간간 10회 제한
                    
                    ### 📝 검증 규칙
                    - **사용자명**: 2-50자
                    - **이메일**: 유효한 이메일 형식
                    - **비밀번호**: 6-20자, 대소문자+숫자+특수문자 포함
                    - **핸드폰번호**: 010-1234-5678 형식
                    - **인증코드**: 6자리 숫자
                    
                    ### 🎯 추후 확장
                    - 소셜 로그인 (Google, Kakao, Naver) 지원 예정
                    - 다중 인증 방식 연동 가능한 구조
                    """,
            tags = {"인증"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ 회원가입 성공",
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
                    description = "❌ 잘못된 요청",
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
                    description = "🚫 Rate Limit 초과",
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
            @Valid @RequestBody UserSignupRequest userSignupRequestDto,
            HttpServletRequest request) {

        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);

        // Rate limiting과 회원가입 로직을 서비스로 위임
        userService.signupWithRateLimit(userSignupRequestDto, clientIp);

        log.info("회원가입 성공: ip={}, email={}", clientIp, userSignupRequestDto.getEmail());
        return ResponseEntity.ok(
                UserSignupResponse.success("회원가입이 성공적으로 완료되었습니다.")
        );
    }

    @Operation(
            summary = "로컬 로그인",
            description = """
                    ## 🔐 로컬 계정 로그인
                    
                    이메일과 비밀번호를 사용하여 로컬 계정으로 로그인합니다.
                    
                    ### 🔒 인증 과정
                    1. **이메일 검증**: 가입된 이메일인지 확인
                    2. **비밀번호 검증**: BCrypt 해싱을 통한 비밀번호 확인
                    3. **JWT 토큰 생성**: 24시간 유효한 JWT 토큰 발급
                    4. **응답 헤더**: Authorization 헤더에 Bearer 토큰 포함
                    
                    ### ⚡ Rate Limiting
                    - **제한**: IP당 15분간 5회
                    - **초과 시**: HTTP 429 상태코드와 Retry-After 헤더 반환
                    
                    ### 🛡️ 보안 기능
                    - **비밀번호 해싱**: BCrypt를 통한 안전한 비밀번호 검증
                    - **JWT 토큰**: HMAC-SHA256 알고리즘 사용
                    - **타이밍 공격 방지**: 상수시간 비교 연산
                    - **Rate Limiting**: IP 기반 로그인 시도 제한
                    
                    ### 📝 사용법
                    1. 가입된 이메일과 비밀번호 입력
                    2. JWT 토큰을 응답에서 받음
                    3. 이후 API 호출 시 Authorization 헤더에 포함
                    """,
            tags = {"인증"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ 로그인 성공",
                    content = @Content(
                            schema = @Schema(implementation = UserLoginResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "로그인 성공",
                                    value = """
                                            {
                                                "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "userName": "홍길동",
                                                "userRole": "USER"
                                            }
                                            """
                            )
                    ),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "Authorization",
                            description = "JWT Bearer 토큰",
                            schema = @Schema(type = "string", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ 잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = UserLoginResponse.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "인증 실패",
                                            value = """
                                                    {
                                                        "error": "이메일 또는 비밀번호가 일치하지 않습니다."
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "유효성 검증 실패",
                                            value = """
                                                    {
                                                        "error": "올바른 이메일 형식이 아닙니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "🚫 Rate Limit 초과",
                    content = @Content(
                            schema = @Schema(implementation = UserLoginResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "로그인 속도 제한",
                                    value = """
                                            {
                                                "error": "로그인 시도 속도 제한을 초과했습니다. 900초 후 다시 시도해주세요."
                                            }
                                            """
                            )
                    ),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "Retry-After",
                            description = "다시 시도 가능한 시간(초)",
                            schema = @Schema(type = "integer", example = "900")
                    )
            )
    })
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(
            @Parameter(description = "로그인 요청 정보 (이메일, 비밀번호)", required = true)
            @RequestBody @Valid UserLoginRequest userLoginRequest,
            HttpServletRequest request) {
        
        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);
        
        // Rate limiting 체크
        rateLimitService.checkLoginRateLimit(clientIp);
        
        UserLoginResponse response = userService.loginWithRefreshToken(userLoginRequest, clientIp);
        
        // 보안 강화: 민감한 정보 마스킹 후 로깅
        log.info("로그인 API 완료: ip={}, email={}", 
                LogMaskingUtil.maskIpAddress(clientIp), 
                LogMaskingUtil.maskEmail(userLoginRequest.getEmail()));
        
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + response.getToken())
                .body(response);
    }

    @Operation(
            summary = "토큰 갱신",
            description = """
                    ## 🔄 JWT 토큰 갱신
                    
                    만료된 Access Token을 Refresh Token으로 갱신합니다.
                    
                    ### 🔒 토큰 회전 보안
                    1. **Refresh Token 검증**: 유효성 및 만료 시간 확인
                    2. **새로운 토큰 쌍 발급**: Access + Refresh Token 모두 갱신
                    3. **기존 토큰 무효화**: 이전 Refresh Token 즉시 무효화
                    4. **단일 세션 정책**: 사용자당 하나의 활성 세션
                    
                    ### ⚡ Rate Limiting
                    - **제한**: IP당 15분간 5회 (로그인과 동일)
                    - **초과 시**: HTTP 429 상태코드와 Retry-After 헤더 반환
                    
                    ### 🛡️ 보안 기능
                    - **토큰 회전**: 갱신 시마다 새로운 Refresh Token 발급
                    - **타이밍 공격 방지**: 상수시간 토큰 검증
                    - **단일 사용**: Refresh Token은 한 번만 사용 가능
                    - **자동 무효화**: 의심 활동 감지 시 모든 토큰 무효화
                    
                    ### 📝 사용법
                    1. Authorization 헤더에 Refresh Token 포함
                    2. 새로운 Access Token과 Refresh Token 받기
                    3. 기존 Refresh Token은 즉시 폐기
                    4. 새로운 토큰으로 API 호출 재개
                    """,
            tags = {"인증"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ 토큰 갱신 성공",
                    content = @Content(
                            schema = @Schema(implementation = UserLoginResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "토큰 갱신 성공",
                                    value = """
                                            {
                                                "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "tokenType": "Bearer",
                                                "expiresIn": 900
                                            }
                                            """
                            )
                    ),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "Authorization",
                            description = "새로운 JWT Bearer 토큰",
                            schema = @Schema(type = "string", example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "❌ 인증 실패",
                    content = @Content(
                            schema = @Schema(implementation = UserLoginResponse.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "만료된 토큰",
                                            value = """
                                                    {
                                                        "error": "만료되었거나 유효하지 않은 리프레시 토큰입니다."
                                                    }
                                                    """
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "유효하지 않은 토큰",
                                            value = """
                                                    {
                                                        "error": "유효하지 않은 리프레시 토큰입니다."
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "🚫 Rate Limit 초과",
                    content = @Content(
                            schema = @Schema(implementation = UserLoginResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "토큰 갱신 속도 제한",
                                    value = """
                                            {
                                                "error": "토큰 갱신 속도 제한을 초과했습니다. 900초 후 다시 시도해주세요."
                                            }
                                            """
                            )
                    ),
                    headers = @io.swagger.v3.oas.annotations.headers.Header(
                            name = "Retry-After",
                            description = "다시 시도 가능한 시간(초)",
                            schema = @Schema(type = "integer", example = "900")
                    )
            )
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(
            @Parameter(description = "Authorization 헤더 (Bearer + Refresh Token)", required = true)
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest request) {
        
        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);
        
        // Authorization 헤더에서 토큰 추출
        String refreshToken = jwtConfig.extractTokenFromHeader(authorizationHeader);
        
        if (refreshToken == null) {
            log.warn("토큰 갱신 실패 - Authorization 헤더 없음: ip={}", 
                    LogMaskingUtil.maskIpAddress(clientIp));
            return ResponseEntity.badRequest().body(
                UserLoginResponse.error("Authorization 헤더가 필요합니다.")
            );
        }
        
        // 토큰 갱신 처리
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.refreshTokens(refreshToken, clientIp);
        
        // 보안 강화: 민감한 정보 마스킹 후 로깅
        log.info("토큰 갱신 API 완료: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
        
        // 새로운 토큰 쌍 응답
        Map<String, Object> response = Map.of(
            "accessToken", tokenPair.getAccessToken(),
            "refreshToken", tokenPair.getRefreshToken(),
            "tokenType", "Bearer",
            "expiresIn", 900 // 15분 (초 단위)
        );
        
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + tokenPair.getAccessToken())
                .body(response);
    }

    @Operation(
            summary = "로그아웃",
            description = """
                    ## 🚪 로그아웃
                    
                    현재 세션을 종료하고 모든 토큰을 무효화합니다.
                    
                    ### 🔒 보안 강화
                    - **Access Token 블랙리스트**: 로그아웃 시 Access Token을 블랙리스트에 추가
                    - **Refresh Token 무효화**: Refresh Token을 즉시 무효화
                    - **세션 정리**: 서버에서 토큰 정보 삭제
                    - **로그 기록**: 보안 감사를 위한 로그아웃 이벤트 기록
                    
                    ### 📝 사용법
                    1. Authorization 헤더에 현재 Access Token 포함
                    2. Request Body에 Refresh Token 포함
                    3. 로그아웃 요청 전송
                    4. 클라이언트에서 모든 토큰 삭제
                    """,
            tags = {"인증"}
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "✅ 로그아웃 성공",
                    content = @Content(
                            schema = @Schema(implementation = UserSignupResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "로그아웃 성공",
                                    value = """
                                            {
                                                "success": true,
                                                "message": "로그아웃이 완료되었습니다."
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "❌ 잘못된 요청",
                    content = @Content(
                            schema = @Schema(implementation = UserSignupResponse.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "헤더 누락",
                                    value = """
                                            {
                                                "success": false,
                                                "message": "Authorization 헤더가 필요합니다."
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/logout")  
    public ResponseEntity<UserLoginResponse> logout(
            @Parameter(description = "Authorization 헤더 (Bearer + Access Token)")
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @Parameter(description = "로그아웃 요청 (Refresh Token 포함)")
            @RequestBody Map<String, String> logoutRequest,
            HttpServletRequest request) {
        
        String clientIp = ClientIpUtil.getClientIpAddress(request, isDevelopment);
        
        // 1. Access Token 처리 (Authorization 헤더에서 추출)
        String accessToken = jwtConfig.extractTokenFromHeader(authorizationHeader);
        if (accessToken != null && jwtConfig.validateToken(accessToken) && jwtConfig.isAccessToken(accessToken)) {
            try {
                blacklistService.addAccessTokenToBlacklist(accessToken);
                log.info("Access Token이 블랙리스트에 추가되었습니다: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
            } catch (Exception e) {
                log.warn("Access Token 블랙리스트 추가 실패: ip={}, error={}", 
                        LogMaskingUtil.maskIpAddress(clientIp), e.getMessage());
            }
        }
        
        // 2. Refresh Token 처리 (Request Body에서 추출)
        String refreshToken = logoutRequest.get("refreshToken");
        if (refreshToken != null) {
            try {
                refreshTokenService.revokeRefreshToken(refreshToken);
                log.info("Refresh Token이 무효화되었습니다: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
            } catch (Exception e) {
                log.warn("Refresh Token 무효화 실패: ip={}, error={}", 
                        LogMaskingUtil.maskIpAddress(clientIp), e.getMessage());
            }
        }
        
        log.info("로그아웃 완료: ip={}", LogMaskingUtil.maskIpAddress(clientIp));
        
        return ResponseEntity.ok(
            UserLoginResponse.success("로그아웃이 완료되었습니다.")
        );
    }
}
