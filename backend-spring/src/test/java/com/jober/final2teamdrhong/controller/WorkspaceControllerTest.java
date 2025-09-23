package com.jober.final2teamdrhong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.workspace.WorkspaceRequest;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import jakarta.persistence.EntityManager;
import com.jober.final2teamdrhong.util.test.WithMockJwtClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc; // HTTP 요청을 시뮬레이션하는 객체

    @Autowired
    private ObjectMapper objectMapper; // Java 객체를 JSON 문자열로 변환하는 객체

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        // 기존 데이터 완전 삭제 및 H2 시퀀스 리셋
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("ALTER TABLE workspace ALTER COLUMN workspace_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN users_id RESTART WITH 1");

        // 이제 testUser가 ID=1로 생성됨 (컨트롤러의 하드코딩된 userId=1과 일치)
        testUser = User.builder()
                .userName("테스트유저")
                .userEmail("test@example.com")
                .userNumber("010-1234-5678")
                .build();
        userRepository.save(testUser);

        anotherUser = User.builder()
                .userName("다른유저")
                .userEmail("another@example.com")
                .userNumber("010-5678-1234")
                .build();
        userRepository.save(anotherUser);
    }

    @Test
    @DisplayName("워크스페이스 생성 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void createWorkspace_Success_Test() throws Exception {
        // given
        WorkspaceRequest.CreateDTO createDTO = WorkspaceRequest.CreateDTO.builder()
                .workspaceName("성공 테스트 워크스페이스")
                .workspaceSubname("부이름")
                .workspaceAddress("주소")
                .workspaceDetailAddress("상세주소")
                .workspaceUrl("unique-url-success")
                .representerName("홍길동")
                .representerPhoneNumber("010-1111-2222")
                .representerEmail("gildong@example.com")
                .companyName("성공 주식회사")
                .companyRegisterNumber("111-22-33333")
                .build();

        String requestBody = objectMapper.writeValueAsString(createDTO);

        // when
        // POST /api/workspaces 로 JSON 데이터를 담아 요청을 보냄
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces") // 실제 API 엔드포인트 경로
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        resultActions
                .andExpect(status().isCreated()) // HTTP 상태 코드가 201 Created 인지 확인
                .andExpect(jsonPath("$.workspaceId").exists())
                .andExpect(jsonPath("$.workspaceName").value("성공 테스트 워크스페이스"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.deletedAt").isEmpty());
    }

    @Test
    @DisplayName("워크스페이스 생성 실패 테스트 - 필수 필드 누락")
    @WithMockJwtClaims(userId = 1)
    void createWorkspace_Fail_Validation_Test() throws Exception {
        // given
        WorkspaceRequest.CreateDTO createDTO = WorkspaceRequest.CreateDTO.builder()
                .workspaceName("") // workspaceName을 @NotBlank 위반으로 빈 값으로 설정
                .workspaceSubname("부이름")
                .workspaceAddress("주소")
                .workspaceDetailAddress("상세주소")
                .workspaceUrl("unique-url-fail")
                .representerName("김철수")
                .representerPhoneNumber("010-3333-4444")
                .representerEmail("cheolsoo@example.com")
                .companyName("실패 주식회사")
                .companyRegisterNumber("444-55-66666")
                .build();

        String requestBody = objectMapper.writeValueAsString(createDTO);

        // when
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        resultActions.andExpect(status().isBadRequest()); // 유효성 검사 실패로 400 Bad Request가 반환되는지 확인
    }

    @Test
    @DisplayName("워크스페이스 목록 조회 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void readWorkspaces_Success_Test() throws Exception {
        // given
        // 1. @BeforeEach에서 생성된 testUser의 소유로 워크스페이스 2개를 DB에 미리 저장합니다.
        workspaceRepository.save(Workspace.builder()
                .workspaceName("테스트 워크스페이스1")
                .workspaceUrl("test-url-1")
                .representerName("테스트대표1")
                .representerPhoneNumber("010-1111-1111")
                .companyName("테스트회사1")
                .user(testUser)
                .build());
        workspaceRepository.save(Workspace.builder()
                .workspaceName("테스트 워크스페이스2")
                .workspaceUrl("test-url-2")
                .representerName("테스트대표2")
                .representerPhoneNumber("010-2222-2222")
                .companyName("테스트회사2")
                .user(testUser)
                .build());

        // when
        // 2. MockMvc를 사용해 GET /api/workspaces API를 호출하고, 그 결과를 ResultActions 객체에 저장합니다.
        //    클래스 상단의 @WithMockUser 설정 덕분에 이 요청은 인증된 사용자의 요청으로 처리됩니다.
        ResultActions resultActions = mockMvc.perform(get("/workspaces"));

        // then
        // 3. ResultActions 객체를 사용하여 응답을 검증합니다.
        resultActions
            // 3-1. 응답 상태 코드가 200 OK 인지 확인합니다.
            .andExpect(status().isOk())
            // 3-2. 응답 Body의 최상위($)가 JSON 배열이고, 그 크기가 2인지 확인합니다.
            .andExpect(jsonPath("$", hasSize(2)))
            // 3-3. 배열의 요소의 필드 값들을 검증합니다.
            .andExpect(jsonPath("$[0].workspaceName").value("테스트 워크스페이스1"))
            .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
            .andExpect(jsonPath("$[0].updatedAt").isNotEmpty())
            .andExpect(jsonPath("$[0].deletedAt").isEmpty())
            .andExpect(jsonPath("$[1].workspaceName").value("테스트 워크스페이스2"));
    }

    @Test
    @DisplayName("워크스페이스 목록 조회 실패 테스트 - 존재하지 않는 사용자")
    @WithMockJwtClaims(userId = 999) // DB에 절대 존재하지 않을 ID를 사용
    void readWorkspaces_Fail_UserNotFound_Test() throws Exception {
        // given
        // 이 테스트에서는 별도의 given 데이터가 필요 없습니다.
        // @WithMockJwtClaims가 DB에 없는 사용자(ID: 999)로
        // 인증 정보를 설정해 줄 것이기 때문입니다.

        // when
        // 존재하지 않는 사용자로 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(get("/workspaces"));

        // then
        // 서비스 계층에서 "해당 사용자를 찾을 수 없습니다" 예외가 발생하고,
        // GlobalExceptionHandler에 의해 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("워크스페이스 상세 조회 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void readWorkspaceDetail_Success_Test() throws Exception {
        // given
        // 테스트용 워크스페이스를 생성하고, testUser를 주인으로 설정한 뒤 DB에 저장합니다.
        Workspace testWorkspace1 = Workspace.builder()
                .workspaceName("테스트 워크스페이스1")
                .workspaceUrl("test-url-1")
                .representerName("테스트대표1")
                .representerPhoneNumber("010-1111-1111")
                .companyName("테스트회사1")
                .user(testUser)
                .build();
        workspaceRepository.save(testWorkspace1);

        // when
        // 저장된 워크스페이스의 ID를 경로 변수(PathVariable)로 사용하여 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                get("/workspaces/" + testWorkspace1.getWorkspaceId())
        );

        // then
        // 1. 응답 상태 코드가 200 OK 인지 확인합니다.
        // 2. 응답 Body의 workspaceName 필드 값이 우리가 넣은 데이터와 일치하는지 확인합니다.
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceName").value("테스트 워크스페이스1"));
    }

    @Test
    @DisplayName("워크스페이스 상세 조회 실패 테스트 - 권한 없음")
    @WithMockJwtClaims(userId = 1)
    void readWorkspaceDetail_Fail_Unauthorized_Test() throws Exception {
        // given
        // 내가 아닌 다른 사용자(anotherUser) 소유의 워크스페이스를 DB에 저장합니다.
        Workspace othersWorkspace = Workspace.builder()
                .workspaceName("남의 워크스페이스")
                .workspaceUrl("another-url-1")
                .representerName("김대표")
                .representerPhoneNumber("010-1111-1111")
                .companyName("남의 회사")
                .user(anotherUser)
                .build();
        workspaceRepository.save(othersWorkspace);

        // when
        // 현재 로직상 'testUser'(userId=1)로 간주되는 사용자가 다른 사람(anotherUser)의 워크스페이스 ID로 조회를 시도합니다.
        ResultActions resultActions = mockMvc.perform(
                get("/workspaces/" + othersWorkspace.getWorkspaceId())
        );

        // then
        // 서비스 계층에서 소유자가 달라 조회가 거부되고, 최종적으로 400 Bad Request를 반환하는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("워크스페이스 수정 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void updateWorkspace_Success_Test() throws Exception {
        // given
        // 1. 수정 대상이 될 원본 워크스페이스를 DB에 미리 저장합니다.
        //    이 워크스페이스의 소유자는 @BeforeEach에서 생성된 testUser (ID=1) 입니다.
        Workspace originalWorkspace = Workspace.builder()
                .workspaceName("원본 워크스페이스")
                .workspaceUrl("original-url-for-update")
                .representerName("원본 대표")
                .representerPhoneNumber("010-1111-1111")
                .companyName("원본 회사")
                .user(testUser)
                .build();
        workspaceRepository.save(originalWorkspace);
        // 1-1. 수정 전의 updatedAt 값을 저장해둡니다.
        String originalUpdatedAt = originalWorkspace.getUpdatedAt().toString();

        // 2. API 요청 본문(Body)에 담아 보낼 수정 데이터를 DTO 객체로 준비합니다.
        WorkspaceRequest.UpdateDTO updateDTO = WorkspaceRequest.UpdateDTO.builder()
                .newWorkspaceName("수정된 워크스페이스")
                .newWorkspaceSubname("수정된 부이름")
                .newWorkspaceAddress("수정된 주소")
                .newWorkspaceDetailAddress("수정된 상세주소")
                .newWorkspaceUrl("updated-unique-url")
                .newRepresenterName("수정된 대표")
                .newRepresenterPhoneNumber("010-9999-8888")
                .newRepresenterEmail("updated@example.com")
                .newCompanyName("수정된 회사")
                .newCompanyRegisterNumber("999-88-77777")
                .build();

        // 3. DTO 객체를 JSON 문자열로 변환합니다.
        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // 4. updatedAt 비교를 위해 1초 대기합니다. (BaseEntity에서 초 단위로 truncate하기 때문)
        Thread.sleep(1000);

        // when
        // 1. MockMvc를 사용하여 PUT /workspaces/{workspaceId} 엔드포인트로 API 요청을 보냅니다.
        //    - contentType을 application/json으로 설정합니다.
        //    - content에 위에서 만든 JSON 문자열을 담습니다.
        ResultActions resultActions = mockMvc.perform(
                put("/workspaces/" + originalWorkspace.getWorkspaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. API 호출 결과를 검증합니다.
        //    - HTTP 상태 코드가 200 OK 인지 확인합니다.
        //    - 응답으로 받은 JSON 본문의 필드 값들이 우리가 요청한 수정 데이터와 일치하는지 확인합니다.
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceName").value("수정된 워크스페이스"))
                .andExpect(jsonPath("$.workspaceUrl").value("updated-unique-url"))
                .andExpect(jsonPath("$.companyName").value("수정된 회사"))
                .andExpect(jsonPath("$.updatedAt").value(not(originalUpdatedAt)))
                .andExpect(jsonPath("$.deletedAt").isEmpty());
    }

    @Test
    @DisplayName("워크스페이스 수정 실패 테스트 - 권한 없음")
    @WithMockJwtClaims(userId = 1)
    void updateWorkspace_Fail_Unauthorized_Test() throws Exception {
        // given
        // 1. 다른 사용자(anotherUser) 소유의 워크스페이스를 DB에 저장합니다.
        //    현재 요청을 보내는 사용자는 testUser(ID=1)이므로, 이 워크스페이스에 대한 수정 권한이 없습니다.
        Workspace othersWorkspace = Workspace.builder()
                .workspaceName("남의 워크스페이스")
                .workspaceUrl("another-users-workspace")
                .representerName("김대표")
                .representerPhoneNumber("010-1111-1111")
                .companyName("남의 회사")
                .user(anotherUser)
                .build();
        workspaceRepository.save(othersWorkspace);

        // 2. 요청 본문에 담길 DTO를 준비합니다. 유효성 검사를 통과할 최소한의 데이터만 넣습니다.
        WorkspaceRequest.UpdateDTO updateDTO = WorkspaceRequest.UpdateDTO.builder()
                .newWorkspaceName("수정 시도")
                .newWorkspaceUrl("attempt-update-url")
                .newRepresenterName("대표")
                .newRepresenterPhoneNumber("010-1234-5678")
                .newCompanyName("회사")
                .build();
        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // when
        // 현재 사용자(testUser)가 다른 사람(anotherUser)의 워크스페이스 수정을 시도합니다.
        ResultActions resultActions = mockMvc.perform(
                put("/workspaces/" + othersWorkspace.getWorkspaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. 서비스 계층에서 소유권이 없다고 판단하여 예외를 던지고,
        //    GlobalExceptionHandler에 의해 최종적으로 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("워크스페이스 수정 실패 테스트 - 필수 필드 누락")
    @WithMockJwtClaims(userId = 1)
    void updateWorkspace_Fail_Validation_Test() throws Exception {
        // given
        // 1. 수정 대상 워크스페이스를 하나 생성합니다.
        Workspace targetWorkspace = Workspace.builder()
                .workspaceName("유효성 검사 대상")
                .workspaceUrl("validation-target-url")
                .representerName("대표")
                .representerPhoneNumber("010-1111-1111")
                .companyName("회사")
                .user(testUser)
                .build();
        workspaceRepository.save(targetWorkspace);

        // 2. @NotBlank 제약조건을 위반하는, 비어있는 workspaceName을 가진 DTO를 준비합니다.
        WorkspaceRequest.UpdateDTO invalidUpdateDTO = WorkspaceRequest.UpdateDTO.builder()
                .newWorkspaceName("") // @NotBlank 위반
                .newWorkspaceUrl("valid-url")
                .newRepresenterName("대표")
                .newRepresenterPhoneNumber("010-1234-5678")
                .newCompanyName("회사")
                .build();
        String requestBody = objectMapper.writeValueAsString(invalidUpdateDTO);

        // when
        // 유효하지 않은 데이터로 수정 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                put("/workspaces/" + targetWorkspace.getWorkspaceId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. 컨트롤러의 @Valid 어노테이션에 의해 요청이 서비스 계층으로 전달되기 전에 차단되고,
        //    400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("워크스페이스 삭제 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void deleteWorkspace_Success_Test() throws Exception {
        // given
        // 1. 삭제 대상이 될 워크스페이스를 DB에 미리 저장합니다.
        //    이 워크스페이스의 소유자는 @BeforeEach에서 생성된 testUser (ID=1) 입니다.
        Workspace targetWorkspace = Workspace.builder()
                .workspaceName("삭제될 워크스페이스")
                .workspaceUrl("delete-target-url")
                .representerName("삭제 대표")
                .representerPhoneNumber("010-1111-1111")
                .companyName("삭제 회사")
                .user(testUser)
                .build();
        workspaceRepository.save(targetWorkspace);

        // when
        // MockMvc를 사용하여 DELETE /workspaces/{workspaceId} 엔드포인트로 API 요청을 보냅니다.
        ResultActions resultActions = mockMvc.perform(
                delete("/workspaces/" + targetWorkspace.getWorkspaceId())
        );

        // then
        // 1. API 호출 결과를 검증합니다.
        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value(targetWorkspace.getWorkspaceId()))
                .andExpect(jsonPath("$.workspaceName").value("삭제될 워크스페이스"))
                .andExpect(jsonPath("$.deletedAt").isNotEmpty()); // deletedAt 필드가 null이 아니거나 비어있지 않은지 확인

        // 2. (중요) DB에서 실제로 소프트 딜리트되었는지 확인합니다.
        //    영속성 컨텍스트의 1차 캐시를 비워 DB에서 직접 조회하도록 강제합니다.
        entityManager.flush();
        entityManager.clear();
        //    @SQLRestriction("is_deleted = false") 때문에, 삭제된 엔티티는 findById로 조회되지 않아야 합니다.
        assertFalse(workspaceRepository.findById(targetWorkspace.getWorkspaceId()).isPresent());
    }

    @Test
    @DisplayName("워크스페이스 삭제 실패 테스트 - 권한 없음")
    @WithMockJwtClaims(userId = 1)
    void deleteWorkspace_Fail_Unauthorized_Test() throws Exception {
        // given
        // 1. 다른 사용자(anotherUser) 소유의 워크스페이스를 DB에 저장합니다.
        //    현재 요청을 보내는 사용자는 testUser(ID=1)이므로, 이 워크스페이스에 대한 삭제 권한이 없습니다.
        Workspace othersWorkspace = Workspace.builder()
                .workspaceName("남의 워크스페이스")
                .workspaceUrl("another-users-workspace")
                .representerName("김대표")
                .representerPhoneNumber("010-1111-1111")
                .companyName("남의 회사")
                .user(anotherUser)
                .build();
        workspaceRepository.save(othersWorkspace);

        // when
        // 현재 사용자(testUser)가 다른 사람(anotherUser)의 워크스페이스 삭제를 시도합니다.
        ResultActions resultActions = mockMvc.perform(
                delete("/workspaces/" + othersWorkspace.getWorkspaceId())
        );

        // then
        // 1. 서비스 계층에서 소유권이 없다고 판단하여 예외를 던지고,
        //    GlobalExceptionHandler에 의해 최종적으로 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }
}
