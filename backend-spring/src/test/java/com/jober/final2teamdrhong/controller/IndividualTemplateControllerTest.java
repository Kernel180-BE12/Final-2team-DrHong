package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplatePageableRequest;
import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplateResponse;
import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.entity.User.UserRole;
import com.jober.final2teamdrhong.service.IndividualTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import com.jober.final2teamdrhong.entity.IndividualTemplate;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class IndividualTemplateControllerTest {

    @Mock
    IndividualTemplateService individualTemplateService;

    @InjectMocks
    IndividualTemplateController controller;

    @Test
    void createEmptyTemplate_정상_호출() {
        // given
        JwtClaims mockClaims = createMockJwtClaims(1, "test@test.com");

        IndividualTemplateResponse expectedResponse = new IndividualTemplateResponse();
        ReflectionTestUtils.setField(expectedResponse, "individualTemplateId", 1);
        ReflectionTestUtils.setField(expectedResponse, "workspaceId", 99);

        given(individualTemplateService.createTemplate(99)).willReturn(expectedResponse);

        // when
        ResponseEntity<IndividualTemplateResponse> result =
                controller.createEmptyTemplate(99, mockClaims);

        // then
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getIndividualTemplateId()).isEqualTo(1);
        assertThat(result.getBody().getWorkspaceId()).isEqualTo(99);

        verify(individualTemplateService).validateWorkspaceOwnership(99, 1);
        verify(individualTemplateService).createTemplate(99);
    }

    @Test
    void createEmptyTemplateAsync_정상_호출() {
        // given
        JwtClaims mockClaims = createMockJwtClaims(2, "test@test.com");

        IndividualTemplateResponse expectedResponse = new IndividualTemplateResponse();
        ReflectionTestUtils.setField(expectedResponse, "individualTemplateId", 2);
        ReflectionTestUtils.setField(expectedResponse, "workspaceId", 77);

        given(individualTemplateService.createTemplateAsync(77))
                .willReturn(CompletableFuture.completedFuture(expectedResponse));

        // when
        ResponseEntity<IndividualTemplateResponse> response =
                controller.createEmptyTemplateAsync(77, mockClaims);

        // then
        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIndividualTemplateId()).isEqualTo(2);
        assertThat(response.getBody().getWorkspaceId()).isEqualTo(77);

        verify(individualTemplateService).validateWorkspaceOwnership(77, 2);
        verify(individualTemplateService).createTemplateAsync(77);
    }

    @Test
    void createFromPublicTemplate_정상_호출() {
        // given
        JwtClaims mockClaims = createMockJwtClaims(3, "test@test.com");

        IndividualTemplateResponse expectedResponse = new IndividualTemplateResponse();
        ReflectionTestUtils.setField(expectedResponse, "individualTemplateId", 3);
        ReflectionTestUtils.setField(expectedResponse, "workspaceId", 55);

        given(individualTemplateService.createIndividualTemplateFromPublic(10, 55, 3))
                .willReturn(expectedResponse);

        // when
        ResponseEntity<IndividualTemplateResponse> result =
                controller.createFromPublicTemplate(10, 55, mockClaims);

        // then
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getIndividualTemplateId()).isEqualTo(3);
        assertThat(result.getBody().getWorkspaceId()).isEqualTo(55);

        verify(individualTemplateService).validateWorkspaceOwnership(55, 3);
        verify(individualTemplateService).createIndividualTemplateFromPublic(10, 55, 3);
    }

    @Test
    void createFromPublicTemplateAsync_정상_호출() {
        // given
        JwtClaims mockClaims = createMockJwtClaims(4, "test@test.com");

        IndividualTemplateResponse expectedResponse = new IndividualTemplateResponse();
        ReflectionTestUtils.setField(expectedResponse, "individualTemplateId", 4);
        ReflectionTestUtils.setField(expectedResponse, "workspaceId", 44);

        given(individualTemplateService.createIndividualTemplateFromPublicAsync(20, 44, 4))
                .willReturn(CompletableFuture.completedFuture(expectedResponse));

        // when
        ResponseEntity<IndividualTemplateResponse> result =
                controller.createFromPublicTemplateAsync(20, 44, mockClaims);

        // then
        assertThat(result.getStatusCodeValue()).isEqualTo(200);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getIndividualTemplateId()).isEqualTo(4);
        assertThat(result.getBody().getWorkspaceId()).isEqualTo(44);

        verify(individualTemplateService).validateWorkspaceOwnership(44, 4);
        verify(individualTemplateService).createIndividualTemplateFromPublicAsync(20, 44, 4);
    }

    private JwtClaims createMockJwtClaims(Integer userId, String email) {
        return JwtClaims.builder()
                .userId(userId)
                .email(email)
                .userName("testUser")
                .userRole(UserRole.USER)
                .tokenType("access")
                .jti("test-jti-123")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }
    private IndividualTemplateResponse makeResponse(
            Integer id,
            String title,
            String content,
            String buttonTitle,
            Integer workspaceId,
            boolean isDeleted,
            IndividualTemplate.Status status
    ) {
        LocalDateTime now = LocalDateTime.now();
        // ⚠️ AllArgsConstructor 순서와 정확히 동일해야 함
        return new IndividualTemplateResponse(
                id,                     // individualTemplateId
                title,                  // individualTemplateTitle
                content,                // individualTemplateContent
                buttonTitle,            // buttonTitle
                workspaceId,            // workspaceId
                now.minusMinutes(1),    // createdAt
                now,                    // updatedAt
                isDeleted,              // isDeleted
                status                  // status
        );
    }

    @Test
    @DisplayName("단일 템플릿 조회 성공")
    void getTemplate_정상_호출() {
        Integer workspaceId = 7;
        Integer individualTemplateId = 10;
        Integer userId = 123;
        JwtClaims claims = createMockJwtClaims(userId, "test@example.com");

        IndividualTemplateResponse expected =
                makeResponse(individualTemplateId, "Single Template", "Template Content", null,
                        workspaceId, false, IndividualTemplate.Status.DRAFT);

        given(individualTemplateService.getIndividualTemplate(workspaceId, individualTemplateId))
                .willReturn(expected);

        ResponseEntity<IndividualTemplateResponse> res =
                controller.getTemplate(workspaceId, individualTemplateId, claims);

        assertThat(res.getStatusCodeValue()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getIndividualTemplateId()).isEqualTo(individualTemplateId);
        assertThat(res.getBody().getWorkspaceId()).isEqualTo(workspaceId);
        assertThat(res.getBody().getIndividualTemplateTitle()).isEqualTo("Single Template");
        assertThat(res.getBody().getIndividualTemplateContent()).isEqualTo("Template Content");
        assertThat(res.getBody().getIsDeleted()).isFalse();
        assertThat(res.getBody().getStatus()).isEqualTo(IndividualTemplate.Status.DRAFT);

        verify(individualTemplateService).validateWorkspaceOwnership(workspaceId, userId);
        verify(individualTemplateService).getIndividualTemplate(workspaceId, individualTemplateId);
    }

    @Test
    @DisplayName("단일 템플릿 비동기 조회 성공")
    void getTemplateAsync_정상_호출() throws Exception {
        Integer workspaceId = 8;
        Integer individualTemplateId = 11;
        Integer userId = 777;
        JwtClaims claims = createMockJwtClaims(userId, "async@example.com");

        IndividualTemplateResponse expected =
                makeResponse(individualTemplateId, "Async Single Template", null, null,
                        workspaceId, false, IndividualTemplate.Status.APPROVED);

        given(individualTemplateService.getIndividualTemplateAsync(workspaceId, individualTemplateId))
                .willReturn(CompletableFuture.completedFuture(expected));

        ResponseEntity<IndividualTemplateResponse> res =
                controller.getTemplateAsync(workspaceId, individualTemplateId, claims);

        assertThat(res.getStatusCodeValue()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getStatus()).isEqualTo(IndividualTemplate.Status.APPROVED);

        verify(individualTemplateService).validateWorkspaceOwnership(workspaceId, userId);
        verify(individualTemplateService).getIndividualTemplateAsync(workspaceId, individualTemplateId);
    }

    @Test
    @DisplayName("전체 템플릿 조회 성공 (status=null)")
    void getAllTemplates_정상_호출() {
        Integer workspaceId = 1;
        Integer userId = 42;
        JwtClaims claims = createMockJwtClaims(userId, "all@example.com");

        IndividualTemplatePageableRequest req = new IndividualTemplatePageableRequest();

        IndividualTemplateResponse row =
                makeResponse(1, "Test Template", null, null, workspaceId, false, IndividualTemplate.Status.DRAFT);

        Page<IndividualTemplateResponse> page =
                new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1);

        given(individualTemplateService.getAllTemplates(eq(workspaceId), any(IndividualTemplatePageableRequest.class)))
                .willReturn(page);

        ResponseEntity<Page<IndividualTemplateResponse>> res =
                controller.getAllTemplates(workspaceId, req, claims);

        assertThat(res.getStatusCodeValue()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getContent()).hasSize(1);
        assertThat(res.getBody().getContent().get(0).getIndividualTemplateTitle()).isEqualTo("Test Template");

        verify(individualTemplateService).validateWorkspaceOwnership(workspaceId, userId);
        verify(individualTemplateService).getAllTemplates(eq(workspaceId), any(IndividualTemplatePageableRequest.class));
    }

    @Test
    @DisplayName("상태별 템플릿 조회 성공 (status=DRAFT)")
    void getAllTemplates_Status_DRAFT_정상_호출() {
        Integer workspaceId = 3;
        Integer userId = 314;
        JwtClaims claims = createMockJwtClaims(userId, "status@example.com");

        IndividualTemplatePageableRequest req = new IndividualTemplatePageableRequest();
        req.setStatus(IndividualTemplate.Status.DRAFT);

        IndividualTemplateResponse row =
                makeResponse(3, null, null, null, workspaceId, false, IndividualTemplate.Status.DRAFT);

        Page<IndividualTemplateResponse> page =
                new PageImpl<>(List.of(row), PageRequest.of(0, 10), 1);

        given(individualTemplateService.getAllTemplates(eq(workspaceId), any(IndividualTemplatePageableRequest.class)))
                .willReturn(page);

        ResponseEntity<Page<IndividualTemplateResponse>> res =
                controller.getAllTemplates(workspaceId, req, claims);

        assertThat(res.getStatusCodeValue()).isEqualTo(200);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getContent()).hasSize(1);
        assertThat(res.getBody().getContent().get(0).getStatus()).isEqualTo(IndividualTemplate.Status.DRAFT);

        verify(individualTemplateService).validateWorkspaceOwnership(workspaceId, userId);
        verify(individualTemplateService).getAllTemplates(eq(workspaceId), any(IndividualTemplatePageableRequest.class));
    }
}
