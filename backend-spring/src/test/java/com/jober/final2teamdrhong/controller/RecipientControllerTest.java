package com.jober.final2teamdrhong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.recipient.RecipientRequest;
import com.jober.final2teamdrhong.entity.Recipient;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.RecipientRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecipientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private RecipientRepository recipientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Workspace testWorkspace;

    @BeforeEach
    void setUp() {
        // 1. 각 테스트 실행 전, 데이터베이스를 깨끗한 상태로 만들기 위해 모든 관련 테이블의 데이터를 삭제합니다.
        recipientRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        // 2. H2 DB의 ID 시퀀스를 초기화하여 항상 일관된 ID로 테스트를 시작하도록 보장합니다.
        jdbcTemplate.execute("ALTER TABLE recipient ALTER COLUMN recipient_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE workspace ALTER COLUMN workspace_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN users_id RESTART WITH 1");

        // 3. 테스트에서 사용할 기본 사용자(ID=1)와 워크스페이스(ID=1)를 생성하고 DB에 저장합니다.
        //    이는 컨트롤러의 하드코딩된 currentUserId = 1 과 일치시키기 위함입니다.
        testUser = User.builder()
                .userName("테스트유저")
                .userEmail("test@example.com")
                .userNumber("010-1234-5678")
                .build();
        userRepository.save(testUser);

        testWorkspace = Workspace.builder()
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl("test-url")
                .representerName("대표")
                .representerPhoneNumber("010-1111-2222")
                .companyName("테스트 회사")
                .user(testUser)
                .build();
        workspaceRepository.save(testWorkspace);
    }

    @Test
    @DisplayName("수신자 생성 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void createRecipient_Success_Test() throws Exception {
        // given
        // 1. API 요청 본문에 담아 보낼 DTO 객체를 생성합니다.
        RecipientRequest.CreateDTO createDTO = RecipientRequest.CreateDTO.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1234-5678")
                .recipientMemo("VIP 고객")
                .build();

        // 2. DTO 객체를 JSON 문자열로 변환합니다.
        String requestBody = objectMapper.writeValueAsString(createDTO);

        // when
        // 1. MockMvc를 사용하여 POST /workspaces/{workspaceId}/recipients 엔드포인트로 API 요청을 보냅니다.
        //    - contentType을 application/json으로 설정합니다.
        //    - content에 위에서 만든 JSON 문자열을 담습니다.
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces/" + testWorkspace.getWorkspaceId() + "/recipients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. API 호출 결과를 검증합니다.
        resultActions
                // 1-1. HTTP 상태 코드가 201 Created 인지 확인합니다.
                .andExpect(status().isCreated())
                // 1-2. 응답 JSON 본문에 recipientId 필드가 존재하는지 확인합니다.
                .andExpect(jsonPath("$.recipientId").exists())
                // 1-3. recipientName 필드의 값이 요청한 데이터와 일치하는지 확인합니다.
                .andExpect(jsonPath("$.recipientName").value("홍길동"))
                // 1-4. recipientPhoneNumber 필드의 값이 요청한 데이터와 일치하는지 확인합니다.
                .andExpect(jsonPath("$.recipientPhoneNumber").value("010-1234-5678"))
                // 1-5. 시스템컬럼 필드(createdAt, updatedAt)가 null이 아닌 값, deletedAt은 null로 채워져 있는지 확인합니다.
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.deletedAt").isEmpty());
    }

    @Test
    @DisplayName("수신자 생성 실패 테스트 - 필수 필드 누락")
    @WithMockJwtClaims(userId = 1)
    void createRecipient_Fail_Validation_Test() throws Exception {
        // given
        // 1. DTO의 @NotBlank 제약조건을 위반하는, 비어있는 recipientName을 가진 DTO를 준비합니다.
        RecipientRequest.CreateDTO createDTO = RecipientRequest.CreateDTO.builder()
                .recipientName("") // @NotBlank 위반
                .recipientPhoneNumber("010-1234-5678")
                .build();

        String requestBody = objectMapper.writeValueAsString(createDTO);

        // when
        // 1. 유효하지 않은 데이터로 생성 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces/" + testWorkspace.getWorkspaceId() + "/recipients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. 컨트롤러의 @Valid 어노테이션에 의해 요청이 서비스 계층으로 전달되기 전에 차단되고,
        //    HTTP 상태 코드 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("수신자 생성 실패 테스트 - 권한 없음")
    @WithMockJwtClaims(userId = 1)
    void createRecipient_Fail_UnauthorizedWorkspace_Test() throws Exception {
        // given
        // 1. 존재하지 않거나 내 소유가 아닌 워크스페이스 ID를 임의로 준비합니다.
        Integer unauthorizedWorkspaceId = 999;

        // 2. 요청 본문에 담길 DTO를 준비합니다.
        RecipientRequest.CreateDTO createDTO = RecipientRequest.CreateDTO.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1234-5678")
                .build();

        String requestBody = objectMapper.writeValueAsString(createDTO);

        // when
        // 1. 다른 사람의 워크스페이스에 수신자 추가를 시도하는 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces/" + unauthorizedWorkspaceId + "/recipients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. 서비스 계층의 인가 로직에서 예외를 던지고, GlobalExceptionHandler에 의해
        //    최종적으로 HTTP 상태 코드 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("수신자 목록 페이징 조회 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void readRecipients_Paging_Success_Test() throws Exception {
        // given
        // 1. setUp()에서 생성된 testWorkspace에 테스트용 수신자 2명을 추가로 저장합니다.
        recipientRepository.save(Recipient.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1111-1111")
                .workspace(testWorkspace)
                .build());
        recipientRepository.save(Recipient.builder()
                .recipientName("임꺽정")
                .recipientPhoneNumber("010-2222-2222")
                .workspace(testWorkspace)
                .build());

        // when
        // 1. MockMvc를 사용하여 GET 요청을 보냅니다.
        //    - URL에 page, size, sort 쿼리 파라미터를 추가하여 페이징을 요청합니다.
        ResultActions resultActions = mockMvc.perform(
                get("/workspaces/" + testWorkspace.getWorkspaceId() + "/recipients")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "recipientName,asc") // 이름 오름차순 정렬
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        // 1. API 호출 결과를 검증합니다. 응답 JSON 구조가 Page 객체 형식에 맞는지 확인합니다.
        resultActions
                // 1-1. HTTP 상태 코드가 200 OK 인지 확인합니다.
                .andExpect(status().isOk())
                // 1-2. Page 객체의 totalElements 필드가 2인지 확인합니다.
                .andExpect(jsonPath("$.totalElements").value(2))
                // 1-3. Page 객체의 content 배열의 크기가 2인지 확인합니다.
                .andExpect(jsonPath("$.content.length()").value(2))
                // 1-4. 정렬 결과 확인: 이름 오름차순이므로 "임꺽정"이 첫 번째 요소여야 합니다.
                .andExpect(jsonPath("$.content[0].recipientName").value("임꺽정"))
                // 1-5. 두 번째 요소의 이름이 "홍길동"인지 확인합니다.
                .andExpect(jsonPath("$.content[1].recipientName").value("홍길동"));
    }

    @Test
    @DisplayName("수신자 목록 페이징 조회 실패 테스트 - 권한 없음")
    @WithMockJwtClaims(userId = 1)
    void readRecipients_Paging_Fail_UnauthorizedWorkspace_Test() throws Exception {
        // given
        // 1. 존재하지 않거나 내 소유가 아닌 워크스페이스 ID를 임의로 준비합니다.
        Integer unauthorizedWorkspaceId = 999;

        // when
        // 1. 다른 사람의 워크스페이스에 속한 수신자 목록 조회를 시도하는 API를 호출합니다.
        //    (실패 케이스이므로 페이징 파라미터는 중요하지 않습니다.)
        ResultActions resultActions = mockMvc.perform(
                get("/workspaces/" + unauthorizedWorkspaceId + "/recipients")
                        .accept(MediaType.APPLICATION_JSON)
        );

        // then
        // 1. 서비스 계층의 인가 로직에서 예외를 던지고, GlobalExceptionHandler에 의해
        //    최종적으로 HTTP 상태 코드 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("수신자 정보 수정 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void updateRecipient_Success_Test() throws Exception {
        // given
        // 1. DB에 수정 대상이 될 원본 수신자 데이터를 미리 저장합니다.
        Recipient savedRecipient = recipientRepository.save(Recipient.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1111-2222")
                .workspace(testWorkspace)
                .build());
        // 1-1. 수정 전의 updatedAt 값을 저장해둡니다.
        String originalUpdatedAt = savedRecipient.getUpdatedAt().toString();

        // 2. API 요청 본문에 담아 보낼 수정용 DTO 객체를 생성합니다.
        RecipientRequest.UpdateDTO updateDTO = RecipientRequest.UpdateDTO.builder()
                .newRecipientName("김길동")
                .newRecipientPhoneNumber("010-9999-8888")
                .newRecipientMemo("수정된 메모")
                .build();

        // 3. DTO 객체를 JSON 문자열로 변환합니다.
        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // when
        // 1. MockMvc를 사용하여 PUT /workspaces/{workspaceId}/recipients/{recipientId} 엔드포인트로 API 요청을 보냅니다.
        ResultActions resultActions = mockMvc.perform(
                put("/workspaces/" + testWorkspace.getWorkspaceId() + "/recipients/" +
                        savedRecipient.getRecipientId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. API 호출 결과를 검증합니다.
        resultActions
                // 1-1. HTTP 상태 코드가 200 OK 인지 확인합니다.
                .andExpect(status().isOk())
                // 1-2. 응답 JSON의 recipientId가 수정한 ID와 일치하는지 확인합니다.
                .andExpect(jsonPath("$.recipientId").value(savedRecipient.getRecipientId()))
                // 1-3. 응답 JSON의 recipientName이 수정한 값으로 변경되었는지 확인합니다.
                .andExpect(jsonPath("$.recipientName").value("김길동"))
                // 1-4. 응답 JSON의 recipientPhoneNumber가 수정한 값으로 변경되었는지 확인합니다.
                .andExpect(jsonPath("$.recipientPhoneNumber").value("010-9999-8888"))
                // 1-5. 응답 JSON의 updatedAt 값이 원본 값과 다른지 (즉, 갱신되었는지) 확인합니다.
                .andExpect(jsonPath("$.updatedAt").value(not(originalUpdatedAt)));
    }

    @Test
    @DisplayName("수신자 정보 수정 실패 테스트 - 필수 필드 누락")
    @WithMockJwtClaims(userId = 1)
    void updateRecipient_Fail_Validation_Test() throws Exception {
        // given
        // 1. DB에 수정 대상 데이터를 미리 저장합니다.
        Recipient savedRecipient = recipientRepository.save(Recipient.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1111-2222")
                .workspace(testWorkspace)
                .build());

        // 2. DTO의 @NotBlank 제약조건을 위반하는, 비어있는 newRecipientName을 가진 DTO를 준비합니다.
        RecipientRequest.UpdateDTO updateDTO = RecipientRequest.UpdateDTO.builder()
                .newRecipientName("") // @NotBlank 위반
                .newRecipientPhoneNumber("010-9999-8888")
                .build();

        String requestBody = objectMapper.writeValueAsString(updateDTO);

        // when
        // 1. 유효하지 않은 데이터로 수정 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                put("/workspaces/" + testWorkspace.getWorkspaceId() + "/recipients/" +
                        savedRecipient.getRecipientId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. 컨트롤러의 @Valid 어노테이션에 의해 요청이 차단되고,
        //    HTTP 상태 코드 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("수신자 삭제 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void deleteRecipient_Success_Test() throws Exception {
        // given
        // 1. DB에 삭제 대상이 될 수신자 데이터를 미리 저장합니다.
        Recipient savedRecipient = recipientRepository.save(Recipient.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1111-2222")
                .workspace(testWorkspace)
                .build());

        // when
        // 1. MockMvc를 사용하여 DELETE /workspaces/{workspaceId}/recipients/{recipientId} 엔드포인트로 API 요청을 보냅니다.
        ResultActions resultActions = mockMvc.perform(
                delete("/workspaces/" + testWorkspace.getWorkspaceId() + "/recipients/" +
                        savedRecipient.getRecipientId())
        );

        // then
        // 1. API 호출 결과를 검증합니다.
        resultActions
                // 1-1. HTTP 상태 코드가 200 OK 인지 확인합니다.
                .andExpect(status().isOk())
                // 1-2. 응답 JSON의 recipientId가 삭제한 ID와 일치하는지 확인합니다.
                .andExpect(jsonPath("$.recipientId").value(savedRecipient.getRecipientId()))
                // 1-3. 응답 JSON의 deletedAt 필드가 null이 아닌 값으로 채워져 있는지 확인합니다.
                .andExpect(jsonPath("$.deletedAt").isNotEmpty());

        // 2. (중요) DB에서 실제로 소프트 딜리트되었는지 확인합니다.
        //    영속성 컨텍스트의 1차 캐시를 비워 DB에서 직접 조회하도록 강제합니다.
        entityManager.flush();
        entityManager.clear();
        //    @SQLRestriction("is_deleted = false") 때문에, 삭제된 엔티티는 findById로 조회되지 않아야 합니다.
        assertFalse(recipientRepository.findById(savedRecipient.getRecipientId()).isPresent());
    }

    @Test
    @DisplayName("수신자 삭제 실패 테스트 - 권한 없음")
    @WithMockJwtClaims(userId = 1)
    void deleteRecipient_Fail_UnauthorizedWorkspace_Test() throws Exception {
        // given
        // 1. DB에 테스트용 수신자를 저장합니다.
        Recipient savedRecipient = recipientRepository.save(Recipient.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1111-2222")
                .workspace(testWorkspace)
                .build());
        // 2. 존재하지 않거나 내 소유가 아닌 워크스페이스 ID를 임의로 준비합니다.
        Integer unauthorizedWorkspaceId = 999;

        // when
        // 1. 다른 사람의 워크스페이스에 속한 수신자를 삭제하려는 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                delete("/workspaces/" + unauthorizedWorkspaceId + "/recipients/" +
                        savedRecipient.getRecipientId())
        );

        // then
        // 1. 서비스 계층의 인가 로직에서 예외를 던지고, GlobalExceptionHandler에 의해
        //    최종적으로 HTTP 상태 코드 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }
}
