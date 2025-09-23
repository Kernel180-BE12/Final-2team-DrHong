package com.jober.final2teamdrhong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.phonebook.PhoneBookRequest;
import com.jober.final2teamdrhong.entity.*;
import com.jober.final2teamdrhong.repository.PhoneBookRepository;
import com.jober.final2teamdrhong.repository.RecipientRepository;
import com.jober.final2teamdrhong.repository.UserRepository;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import com.jober.final2teamdrhong.util.test.WithMockJwtClaims;
import jakarta.persistence.EntityManager;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PhoneBookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private PhoneBookRepository phoneBookRepository;

    @Autowired
    private RecipientRepository recipientRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Workspace testWorkspace;
    private Recipient recipient1, recipient2, recipient3;

    @BeforeEach
    void setUp() {
        // 1. 각 테스트 실행 전, 데이터베이스를 깨끗한 상태로 만들기 위해 모든 관련 테이블의 데이터를 삭제합니다.
        phoneBookRepository.deleteAll();
        recipientRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
        // 2. H2 DB의 ID 시퀀스를 초기화하여 항상 일관된 ID로 테스트를 시작하도록 보장합니다.
        jdbcTemplate.execute("ALTER TABLE phone_book ALTER COLUMN phone_book_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE recipient ALTER COLUMN recipient_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE workspace ALTER COLUMN workspace_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN users_id RESTART WITH 1");

        // 3. 테스트에서 사용할 기본 사용자(ID=1)와 워크스페이스(ID=1)를 생성하고 DB에 저장합니다.
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

        // 4. 테스트용 수신자들을 생성하고 저장합니다.
        recipient1 = Recipient.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1111-1111")
                .workspace(testWorkspace)
                .build();
        recipient2 = Recipient.builder()
                .recipientName("임꺽정")
                .recipientPhoneNumber("010-1111-1111")
                .workspace(testWorkspace)
                .build();
        recipient3 = Recipient.builder()
                .recipientName("김철수")
                .recipientPhoneNumber("010-1111-1111")
                .workspace(testWorkspace)
                .build();
        recipientRepository.saveAll(List.of(recipient1, recipient2, recipient3));
    }

    @Test
    @DisplayName("주소록 생성 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void createPhoneBook_Success_Test() throws Exception {
        // given
        // 1. API 요청 본문에 담아 보낼 DTO 객체를 생성합니다.
        PhoneBookRequest.CreateDTO createDTO = PhoneBookRequest.CreateDTO.builder()
                .phoneBookName("영업팀 주소록")
                .phoneBookMemo("영업팀 전체 연락처입니다.")
                .build();

        // 2. DTO 객체를 JSON 문자열로 변환합니다.
        String requestBody = objectMapper.writeValueAsString(createDTO);

        // when
        // 1. MockMvc를 사용하여 POST /workspaces/{workspaceId}/phonebooks 엔드포인트로 API 요청을 보냅니다.
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces/" + testWorkspace.getWorkspaceId() + "/phonebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. API 호출 결과를 검증합니다.
        resultActions
                // 1-1. HTTP 상태 코드가 201 Created 인지 확인합니다.
                .andExpect(status().isCreated())
                // 1-2. 응답 JSON 본문에 phoneBookId 필드가 존재하는지 확인합니다.
                .andExpect(jsonPath("$.phoneBookId").exists())
                // 1-3. phoneBookName 필드의 값이 요청한 데이터와 일치하는지 확인합니다.
                .andExpect(jsonPath("$.phoneBookName").value("영업팀 주소록"))
                // 1-4. 시스템컬럼 필드(createdAt, updatedAt)가 null이 아닌 값, deletedAt은 null로 채워져 있는지 확인합니다.
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.deletedAt").isEmpty());
    }

    @Test
    @DisplayName("주소록 생성 실패 테스트 - 필수 필드 누락")
    @WithMockJwtClaims(userId = 1)
    void createPhoneBook_Fail_Validation_Test() throws Exception {
        // given
        // 1. DTO의 @NotBlank 제약조건을 위반하는, 비어있는 phoneBookName을 가진 DTO를 준비합니다.
        PhoneBookRequest.CreateDTO createDTO = PhoneBookRequest.CreateDTO.builder()
                .phoneBookName("") // @NotBlank 위반
                .build();

        String requestBody = objectMapper.writeValueAsString(createDTO);

        // when
        // 1. 유효하지 않은 데이터로 생성 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces/" + testWorkspace.getWorkspaceId() + "/phonebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. 컨트롤러의 @Valid 어노테이션에 의해 요청이 서비스 계층으로 전달되기 전에 차단되고,
        //    HTTP 상태 코드 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주소록 생성 실패 테스트 - 권한 없음")
    @WithMockJwtClaims(userId = 1)
    void createPhoneBook_Fail_UnauthorizedWorkspace_Test() throws Exception {
        // given
        // 1. 존재하지 않거나 내 소유가 아닌 워크스페이스 ID를 임의로 준비합니다.
        Integer unauthorizedWorkspaceId = 999;

        // 2. 요청 본문에 담길 DTO를 준비합니다.
        PhoneBookRequest.CreateDTO createDTO = PhoneBookRequest.CreateDTO.builder()
                .phoneBookName("해킹 시도 주소록")
                .build();

        String requestBody = objectMapper.writeValueAsString(createDTO);

        // when
        // 1. 다른 사람의 워크스페이스에 주소록 추가를 시도하는 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces/" + unauthorizedWorkspaceId + "/phonebooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. 서비스 계층의 인가 로직에서 예외를 던지고, GlobalExceptionHandler에 의해
        //    최종적으로 HTTP 상태 코드 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주소록에 수신자 일괄 추가 성공 테스트")
    @WithMockJwtClaims(userId = 1)
    void addRecipientsToPhoneBook_Success_Test() throws Exception {
        // given
        // 1. 테스트용 주소록을 생성하고 저장합니다.
        PhoneBook phoneBook = PhoneBook.builder()
                .phoneBookName("테스트 주소록")
                .workspace(testWorkspace)
                .build();
        phoneBookRepository.save(phoneBook);

        // 2. 이 주소록에 이미 recipient1이 포함되어 있다고 가정하고, GroupMapping을 설정합니다.
        entityManager.persist(GroupMapping.builder()
                .phoneBook(phoneBook)
                .recipient(recipient1)
                .build());
        entityManager.flush();
        entityManager.clear();

        // 3. API 요청 본문에 recipient1(중복)과 recipient2(신규)를 추가하도록 요청합니다.
        PhoneBookRequest.RecipientIdListDTO requestDTO = new PhoneBookRequest.RecipientIdListDTO(
                List.of(recipient1.getRecipientId(), recipient2.getRecipientId())
        );
        String requestBody = objectMapper.writeValueAsString(requestDTO);

        // when
        // 1. MockMvc를 사용하여 수신자 일괄 추가 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces/{workspaceId}/phonebooks/{phoneBookId}/recipients",
                        testWorkspace.getWorkspaceId(), phoneBook.getPhoneBookId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. API 호출 결과를 검증합니다.
        resultActions
                // 2-1. HTTP 상태 코드가 200 OK인지 확인합니다.
                .andExpect(status().isOk())
                // 2-2. 응답 JSON의 'phoneBookId'가 예상과 일치하는지 확인합니다.
                .andExpect(jsonPath("$.phoneBookId").value(phoneBook.getPhoneBookId()))
                // 2-3. 실제로 추가된 수신자 목록('recipientList')의 길이가 1인지 확인합니다. (중복된 recipient1은 제외)
                .andExpect(jsonPath("$.recipientList.length()").value(1))
                // 2-4. 추가된 수신자가 recipient2가 맞는지 확인합니다.
                .andExpect(jsonPath("$.recipientList[0].recipientId").value(recipient2.getRecipientId()));
    }

    @Test
    @DisplayName("주소록에 수신자 일괄 추가 실패 테스트 - 존재하지 않는 수신자 ID 포함")
    @WithMockJwtClaims(userId = 1)
    void addRecipientsToPhoneBook_Fail_RecipientNotFound_Test() throws Exception {
        // given
        // 1. 테스트용 주소록을 생성합니다.
        PhoneBook phoneBook = PhoneBook.builder()
                .phoneBookName("테스트 주소록")
                .workspace(testWorkspace)
                .build();
        phoneBookRepository.save(phoneBook);

        // 2. 요청 본문에 존재하지 않는 ID(-1)를 포함시킵니다.
        Integer nonExistentRecipientId = -1;
        PhoneBookRequest.RecipientIdListDTO requestDTO = new PhoneBookRequest.RecipientIdListDTO(
                List.of(recipient1.getRecipientId(), nonExistentRecipientId)
        );
        String requestBody = objectMapper.writeValueAsString(requestDTO);

        // when
        // 1. 유효하지 않은 데이터로 API를 호출합니다.
        ResultActions resultActions = mockMvc.perform(
                post("/workspaces/{workspaceId}/phonebooks/{phoneBookId}/recipients",
                        testWorkspace.getWorkspaceId(), phoneBook.getPhoneBookId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );

        // then
        // 1. RecipientValidator에서 예외가 발생하고, GlobalExceptionHandler에 의해
        //    HTTP 상태 코드 400 Bad Request가 반환되는지 확인합니다.
        resultActions.andExpect(status().isBadRequest());
    }
}
