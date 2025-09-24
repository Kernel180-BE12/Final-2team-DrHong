package com.jober.final2teamdrhong.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.favorite.IndividualTemplateFavoriteRequest;
import com.jober.final2teamdrhong.dto.favorite.PublicTemplateFavoriteRequest;
import com.jober.final2teamdrhong.entity.*;
import com.jober.final2teamdrhong.repository.*;
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
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class FavoriteCreationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FavoriteRepository favoriteRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private IndividualTemplateRepository individualTemplateRepository;
    @Autowired
    private PublicTemplateRepository publicTemplateRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Workspace savedWorkspace;
    private IndividualTemplate savedIndividualTemplate;
    private PublicTemplate savedPublicTemplate;

    @BeforeEach
    void setUp() {
        // 모든 데이터 삭제
        favoriteRepository.deleteAll();
        individualTemplateRepository.deleteAll();
        publicTemplateRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();

        // H2 데이터베이스의 ID 시퀀스를 1로 리셋
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN users_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE workspace ALTER COLUMN workspace_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE individual_template ALTER COLUMN individual_template_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE public_template ALTER COLUMN public_template_id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE favorite ALTER COLUMN favorite_id RESTART WITH 1");

        // testUser 항상 ID=1로 생성
        User testUser = User.builder()
                .userName("테스트유저")
                .userEmail("test@example.com")
                .userNumber("010-1111-1111")
                .build();
        userRepository.save(testUser);

        // workspace는 항상 ID=1인 유저 소유
        Workspace workspace = Workspace.builder()
                .user(testUser)
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl("test-url-unique")
                .representerName("홍길동")
                .representerPhoneNumber("010-0000-0000")
                .companyName("테스트 회사")
                .build();

        savedWorkspace = workspaceRepository.save(workspace);

        IndividualTemplate individualTemplate = IndividualTemplate.builder()
                .workspace(savedWorkspace)
                .individualTemplateTitle("테스트 개인 템플릿")
                .individualTemplateContent("테스트 내용입니다.")
                .buttonTitle("확인")
                .build();
        savedIndividualTemplate = individualTemplateRepository.save(individualTemplate);

        PublicTemplate publicTemplate = PublicTemplate.builder()
                .publicTemplateTitle("테스트 공용 템플릿")
                .publicTemplateContent("테스트 내용입니다.")
                .buttonTitle("확인")
                .build();
        savedPublicTemplate = publicTemplateRepository.save(publicTemplate);
    }

    // ====================== Create ======================
    @Test
    @DisplayName("성공 : 개인 템플릿 즐겨찾기 생성")
    @WithMockJwtClaims
    void createIndividualTemplateFavorite_Success() throws Exception {
        IndividualTemplateFavoriteRequest request = new IndividualTemplateFavoriteRequest(
                savedWorkspace.getWorkspaceId(),
                savedIndividualTemplate.getIndividualTemplateId()
        );
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/individual/favorite")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.favoriteId").isNumber());
    }

    @Test
    @DisplayName("실패 : 중복된 즐겨찾기 생성 시 400 Bad Request 응답")
    @WithMockJwtClaims
    void createIndividualTemplateFavorite_Fail_AlreadyExists() throws Exception {
        Favorite existingFavorite = Favorite.builder()
                .workspace(savedWorkspace)
                .individualTemplate(savedIndividualTemplate)
                .build();
        favoriteRepository.save(existingFavorite);

        IndividualTemplateFavoriteRequest request = new IndividualTemplateFavoriteRequest(
                savedWorkspace.getWorkspaceId(),
                savedIndividualTemplate.getIndividualTemplateId()
        );
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/individual/favorite")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("성공(공용) : 공용 템플릿 즐겨찾기 생성")
    @WithMockJwtClaims
    void createPublicTemplateFavorite_Integration_Success() throws Exception {
        PublicTemplateFavoriteRequest request = new PublicTemplateFavoriteRequest(
                savedWorkspace.getWorkspaceId(),
                savedPublicTemplate.getPublicTemplateId()
        );
        String requestBody = objectMapper.writeValueAsString(request);
        long initialCount = favoriteRepository.count();

        mockMvc.perform(post("/public/favorite")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.favoriteId").isNumber())
                .andExpect(jsonPath("$.templateType").value("PUBLIC"))
                .andExpect(jsonPath("$.templateId").value(savedPublicTemplate.getPublicTemplateId()));

        assertThat(favoriteRepository.count()).isEqualTo(initialCount + 1);
    }

    @Test
    @DisplayName("실패(공용) : 중복된 공용 즐겨찾기 생성 시 400 Bad Request 응답")
    @WithMockJwtClaims
    void createPublicTemplateFavorite_Integration_AlreadyExists() throws Exception {
        Favorite existingFavorite = Favorite.builder()
                .workspace(savedWorkspace)
                .publicTemplate(savedPublicTemplate)
                .build();
        favoriteRepository.save(existingFavorite);

        PublicTemplateFavoriteRequest request = new PublicTemplateFavoriteRequest(
                savedWorkspace.getWorkspaceId(),
                savedPublicTemplate.getPublicTemplateId()
        );
        String requestBody = objectMapper.writeValueAsString(request);

        mockMvc.perform(post("/public/favorite")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }





    // ====================== Read ======================
    @Test
    @DisplayName("성공(통합): 특정 워크스페이스의 모든 즐겨찾기 목록 조회")
    @WithMockJwtClaims // userId = 1, workspaceId = 1에 대한 권한 있음
    void getFavorites_Success_ReadAll() throws Exception {
        // given
        favoriteRepository.save(Favorite.builder().workspace(savedWorkspace).publicTemplate(savedPublicTemplate).build());
        favoriteRepository.save(Favorite.builder().workspace(savedWorkspace).individualTemplate(savedIndividualTemplate).build());

        // when & then
        mockMvc.perform(get("/favorites")
                        .param("workspaceId", savedWorkspace.getWorkspaceId().toString())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("성공(통합): 특정 워크스페이스의 공용 템플릿 즐겨찾기만 페이징 조회")
    @WithMockJwtClaims // userId = 1, workspaceId = 1에 대한 권한 있음
    void getFavorites_Success_ReadPublicTemplatesWithPaging() throws Exception {
        // given
        favoriteRepository.save(Favorite.builder().workspace(savedWorkspace).publicTemplate(savedPublicTemplate).build());
        favoriteRepository.save(Favorite.builder().workspace(savedWorkspace).individualTemplate(savedIndividualTemplate).build());

        // when & then
        mockMvc.perform(get("/favorites")
                        .param("workspaceId", savedWorkspace.getWorkspaceId().toString())
                        .param("templateType", "PUBLIC")
                        .param("size", "1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].templateType").value("PUBLIC"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("실패(통합): 권한 없는 워크스페이스의 즐겨찾기 조회 시 400 응답")
    @WithMockJwtClaims(userId = 99) // 존재하지 않거나 권한 없는 사용자
    void getFavorites_Fail_UnauthorizedWorkspace() throws Exception {
        // given
        // User ID 1이 소유한 workspaceId=1의 즐겨찾기를 User ID 99가 조회 시도
        favoriteRepository.save(Favorite.builder().workspace(savedWorkspace).publicTemplate(savedPublicTemplate).build());

        // when & then
        mockMvc.perform(get("/favorites")
                        .param("workspaceId", savedWorkspace.getWorkspaceId().toString())
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }
}
