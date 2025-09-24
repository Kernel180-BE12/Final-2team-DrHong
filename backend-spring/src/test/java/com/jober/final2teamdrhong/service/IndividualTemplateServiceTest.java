package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplatePageableRequest;
import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplateResponse;
import com.jober.final2teamdrhong.entity.IndividualTemplate;
import com.jober.final2teamdrhong.entity.PublicTemplate;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.IndividualTemplateRepository;
import com.jober.final2teamdrhong.repository.PublicTemplateRepository;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IndividualTemplateServiceTest {

    private static final Logger log = LoggerFactory.getLogger(IndividualTemplateServiceTest.class);

    @Mock
    private IndividualTemplateRepository individualTemplateRepo;

    @Mock
    private WorkspaceRepository workspaceRepo;

    @Mock
    private PublicTemplateRepository publicTemplateRepo;

    @InjectMocks
    private IndividualTemplateService service;

    private Workspace workspaceMock;

    @BeforeEach
    void setUp() {
        workspaceMock = mock(Workspace.class);
    }

    @Nested
    @DisplayName("validateWorkspaceOwnership")
    class ValidateWorkspaceOwnership {

        @Test
        @DisplayName("userId가 null이면 AccessDeniedException이 발생한다")
        void validateWorkspaceOwnership_nullUserId() {
            // when & then
            assertThatThrownBy(() -> service.validateWorkspaceOwnership(1, null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("인증이 필요합니다.");

            verifyNoInteractions(workspaceRepo);
        }

        @Test
        @DisplayName("워크스페이스 소유권이 없으면 AccessDeniedException이 발생한다")
        void validateWorkspaceOwnership_noOwnership() {
            // given
            when(workspaceRepo.existsByWorkspaceIdAndUser_UserId(1, 100))
                    .thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.validateWorkspaceOwnership(1, 100))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessage("해당 워크스페이스에 접근 권한이 없습니다.");

            verify(workspaceRepo, times(1)).existsByWorkspaceIdAndUser_UserId(1, 100);
            verifyNoMoreInteractions(workspaceRepo);
        }

        @Test
        @DisplayName("워크스페이스 소유권이 있으면 정상적으로 통과한다")
        void validateWorkspaceOwnership_hasOwnership() {
            // given
            when(workspaceRepo.existsByWorkspaceIdAndUser_UserId(1, 100))
                    .thenReturn(true);

            // when & then
            assertThatCode(() -> service.validateWorkspaceOwnership(1, 100))
                    .doesNotThrowAnyException();

            verify(workspaceRepo, times(1)).existsByWorkspaceIdAndUser_UserId(1, 100);
            verifyNoMoreInteractions(workspaceRepo);
        }
    }

    @Nested
    @DisplayName("createTemplate")
    class CreateTemplate {

        @Test
        @DisplayName("유효한 workspaceId면 빈 템플릿을 생성하고 응답을 반환한다")
        void createTemplate_success() {
            // given
            when(workspaceMock.getWorkspaceId()).thenReturn(1);
            when(workspaceRepo.findById(1)).thenReturn(Optional.of(workspaceMock));

            LocalDateTime now = LocalDateTime.now();
            IndividualTemplate savedMock = mock(IndividualTemplate.class);
            when(savedMock.getIndividualTemplateId()).thenReturn(1);
            when(savedMock.getWorkspace()).thenReturn(workspaceMock);
            when(savedMock.getCreatedAt()).thenReturn(now);
            when(savedMock.getUpdatedAt()).thenReturn(now);

            when(individualTemplateRepo.save(any(IndividualTemplate.class))).thenReturn(savedMock);

            // when
            IndividualTemplateResponse res = service.createTemplate(1);

            // then
            assertThat(res).isNotNull();
            assertThat(res.getIndividualTemplateId()).isEqualTo(1);
            assertThat(res.getWorkspaceId()).isEqualTo(1);

            verify(workspaceRepo).findById(1);
            verify(individualTemplateRepo).save(any(IndividualTemplate.class));
        }

        @Test
        @DisplayName("존재하지 않는 workspaceId면 IllegalArgumentException 발생")
        void createTemplate_invalidWorkspace() {
            // given
            when(workspaceRepo.findById(999)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.createTemplate(999))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 workspaceId");
        }
    }

    @Nested
    @DisplayName("createTemplateAsync")
    class CreateTemplateAsync {

        @Test
        @DisplayName("정상 실행 시 CompletableFuture로 결과를 반환한다")
        void createTemplateAsync_success() throws Exception {
            // given
            when(workspaceMock.getWorkspaceId()).thenReturn(1);
            when(workspaceRepo.findById(1)).thenReturn(Optional.of(workspaceMock));

            LocalDateTime now = LocalDateTime.now();
            IndividualTemplate savedMock = mock(IndividualTemplate.class);
            when(savedMock.getIndividualTemplateId()).thenReturn(456);
            when(savedMock.getWorkspace()).thenReturn(workspaceMock);
            when(savedMock.getCreatedAt()).thenReturn(now);
            when(savedMock.getUpdatedAt()).thenReturn(now);

            when(individualTemplateRepo.save(any(IndividualTemplate.class))).thenReturn(savedMock);

            // when
            CompletableFuture<IndividualTemplateResponse> future = service.createTemplateAsync(1);
            IndividualTemplateResponse res = future.get(2, TimeUnit.SECONDS);

            // then
            assertThat(res.getIndividualTemplateId()).isEqualTo(456);
            assertThat(res.getWorkspaceId()).isEqualTo(1);

            verify(workspaceRepo).findById(1);
            verify(individualTemplateRepo).save(any(IndividualTemplate.class));
        }

        @Test
        @DisplayName("유효하지 않은 workspaceId면 예외 발생")
        void createTemplateAsync_invalidWorkspace() {
            // given
            when(workspaceRepo.findById(999)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.createTemplateAsync(999))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("createIndividualTemplateFromPublic")
    class CreateFromPublic {

        @Test
        @DisplayName("공용 템플릿 내용을 복사하여 개인 템플릿을 생성한다")
        void createFromPublic_success() {
            // given
            when(workspaceMock.getWorkspaceId()).thenReturn(10);
            when(workspaceRepo.findByIdOrThrow(10, 7)).thenReturn(workspaceMock);

            PublicTemplate publicMock = mock(PublicTemplate.class);
            when(publicMock.getPublicTemplateTitle()).thenReturn("제목");
            when(publicMock.getPublicTemplateContent()).thenReturn("내용");
            when(publicMock.getButtonTitle()).thenReturn("버튼");

            when(publicTemplateRepo.findByIdOrThrow(99)).thenReturn(publicMock);

            IndividualTemplate savedMock = mock(IndividualTemplate.class);
            when(savedMock.getIndividualTemplateId()).thenReturn(1000);
            when(savedMock.getIndividualTemplateTitle()).thenReturn("제목");
            when(savedMock.getIndividualTemplateContent()).thenReturn("내용");
            when(savedMock.getButtonTitle()).thenReturn("버튼");
            when(savedMock.getWorkspace()).thenReturn(workspaceMock);
            when(savedMock.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(savedMock.getUpdatedAt()).thenReturn(LocalDateTime.now());

            when(individualTemplateRepo.save(any(IndividualTemplate.class))).thenReturn(savedMock);

            // when
            IndividualTemplateResponse res = service.createIndividualTemplateFromPublic(99, 10, 7);

            // then
            assertThat(res.getIndividualTemplateId()).isEqualTo(1000);
            assertThat(res.getWorkspaceId()).isEqualTo(10);
            assertThat(res.getIndividualTemplateTitle()).isEqualTo("제목");
            assertThat(res.getIndividualTemplateContent()).isEqualTo("내용");
            assertThat(res.getButtonTitle()).isEqualTo("버튼");

            verify(publicTemplateRepo).findByIdOrThrow(99);
            verify(workspaceRepo).findByIdOrThrow(10, 7);
            verify(individualTemplateRepo).save(any(IndividualTemplate.class));
        }
    }

    @Nested
    @DisplayName("createIndividualTemplateFromPublicAsync")
    class CreateFromPublicAsync {

        @Test
        @DisplayName("비동기 실행도 정상적으로 PublicTemplate 기반 생성 결과를 반환한다")
        void createFromPublicAsync_success() throws Exception {
            // given
            when(workspaceMock.getWorkspaceId()).thenReturn(20);
            when(workspaceRepo.findByIdOrThrow(20, 8)).thenReturn(workspaceMock);

            PublicTemplate publicMock = mock(PublicTemplate.class);
            when(publicMock.getPublicTemplateTitle()).thenReturn("Async제목");
            when(publicMock.getPublicTemplateContent()).thenReturn("Async내용");
            when(publicMock.getButtonTitle()).thenReturn("Async버튼");

            when(publicTemplateRepo.findByIdOrThrow(200)).thenReturn(publicMock);

            IndividualTemplate savedMock = mock(IndividualTemplate.class);
            when(savedMock.getIndividualTemplateId()).thenReturn(2000);
            when(savedMock.getIndividualTemplateTitle()).thenReturn("Async제목");
            when(savedMock.getIndividualTemplateContent()).thenReturn("Async내용");
            when(savedMock.getButtonTitle()).thenReturn("Async버튼");
            when(savedMock.getWorkspace()).thenReturn(workspaceMock);
            when(savedMock.getCreatedAt()).thenReturn(LocalDateTime.now());
            when(savedMock.getUpdatedAt()).thenReturn(LocalDateTime.now());

            when(individualTemplateRepo.save(any(IndividualTemplate.class))).thenReturn(savedMock);

            // when
            CompletableFuture<IndividualTemplateResponse> future =
                    service.createIndividualTemplateFromPublicAsync(200, 20, 8);
            IndividualTemplateResponse res = future.get(2, TimeUnit.SECONDS);

            // then
            assertThat(res.getIndividualTemplateId()).isEqualTo(2000);
            assertThat(res.getWorkspaceId()).isEqualTo(20);
            assertThat(res.getIndividualTemplateTitle()).isEqualTo("Async제목");

            verify(publicTemplateRepo).findByIdOrThrow(200);
            verify(workspaceRepo).findByIdOrThrow(20, 8);
            verify(individualTemplateRepo).save(any(IndividualTemplate.class));
        }
    }

    // =========================
    // ===== READ 테스트들 =====
    // =========================
    @Nested
    @DisplayName("getAllTemplates")
    class GetAllTemplates {

        @Test
        @DisplayName("워크스페이스별 삭제되지 않은 템플릿 Page를 매핑하여 반환한다")
        void getAllTemplates_success() {
            when(workspaceMock.getWorkspaceId()).thenReturn(5);

            LocalDateTime now = LocalDateTime.now();
            IndividualTemplate e1 = createMockTemplate(10, "t1", "c1", "b1", workspaceMock, now, false);
            IndividualTemplate e2 = createMockTemplate(11, "t2", "c2", "b2", workspaceMock, now, false);

            Page<IndividualTemplate> repoPage = new PageImpl<>(List.of(e1, e2));

            when(individualTemplateRepo.findByWorkspace_WorkspaceId(anyInt(), any(Pageable.class)))
                    .thenReturn(repoPage);

            // given
            IndividualTemplatePageableRequest request = new IndividualTemplatePageableRequest();
            ReflectionTestUtils.setField(request, "page", 0);
            ReflectionTestUtils.setField(request, "size", 10);

            // when
            Page<IndividualTemplateResponse> page = service.getAllTemplates(5, request);

            // then
            assertThat(page.getContent()).hasSize(2);
            assertThat(page.getContent().get(0).getIndividualTemplateId()).isEqualTo(10);
            assertThat(page.getContent().get(0).getWorkspaceId()).isEqualTo(5);
            assertThat(page.getContent().get(0).getIsDeleted()).isFalse();
            assertThat(page.getContent().get(1).getIndividualTemplateId()).isEqualTo(11);
            assertThat(page.getContent().get(1).getWorkspaceId()).isEqualTo(5);

            verify(individualTemplateRepo).findByWorkspace_WorkspaceId(eq(5), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getAllTemplatesAsync")
    class GetAllTemplatesAsync {

        @Test
        @DisplayName("@Async로 Page 매핑 결과를 CompletableFuture로 반환한다")
        void getAllTemplatesAsync_success() throws Exception {
            when(workspaceMock.getWorkspaceId()).thenReturn(7);

            LocalDateTime now = LocalDateTime.now();
            IndividualTemplate e1 = createMockTemplate(20, null, null, null, workspaceMock, now, false);

            Page<IndividualTemplate> repoPage = new PageImpl<>(List.of(e1));

            when(individualTemplateRepo.findByWorkspace_WorkspaceId(anyInt(), any(Pageable.class)))
                    .thenReturn(repoPage);

            IndividualTemplatePageableRequest request = new IndividualTemplatePageableRequest();
            ReflectionTestUtils.setField(request, "page", 0);
            ReflectionTestUtils.setField(request, "size", 10);

            // when
            CompletableFuture<Page<IndividualTemplateResponse>> future =
                    service.getAllTemplatesAsync(7, request);

            // then
            Page<IndividualTemplateResponse> page = future.get(2, TimeUnit.SECONDS);

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getIndividualTemplateId()).isEqualTo(20);
            assertThat(page.getContent().get(0).getWorkspaceId()).isEqualTo(7);
            assertThat(page.getContent().get(0).getIsDeleted()).isFalse();

            verify(individualTemplateRepo).findByWorkspace_WorkspaceId(eq(7), any(Pageable.class));
        }
    }


    @Nested
    @DisplayName("getIndividualTemplate")
    class GetIndividualTemplate {

        @Test
        @DisplayName("개인 템플릿 단건을 매핑하여 반환한다")
        void getIndividualTemplate_success() {
            when(workspaceMock.getWorkspaceId()).thenReturn(3);

            LocalDateTime now = LocalDateTime.now();
            IndividualTemplate e = createMockTemplate(33, "title-33", "content-33", "button-33", workspaceMock, now, false);

            when(individualTemplateRepo.findByIndividualTemplateIdAndWorkspace_WorkspaceId(33, 3))
                    .thenReturn(Optional.of(e));

            // when
            IndividualTemplateResponse res = service.getIndividualTemplate(3, 33);

            // then
            assertThat(res.getIndividualTemplateId()).isEqualTo(33);
            assertThat(res.getWorkspaceId()).isEqualTo(3);
            assertThat(res.getIndividualTemplateTitle()).isEqualTo("title-33");
            assertThat(res.getIndividualTemplateContent()).isEqualTo("content-33");
            assertThat(res.getButtonTitle()).isEqualTo("button-33");
            assertThat(res.getIsDeleted()).isFalse();
            assertThat(res.getCreatedAt()).isEqualTo(now);
            assertThat(res.getUpdatedAt()).isEqualTo(now);

            verify(individualTemplateRepo, times(1))
                    .findByIndividualTemplateIdAndWorkspace_WorkspaceId(33, 3);
        }

        @Test
        @DisplayName("조회 대상이 없으면 IllegalArgumentException을 던진다")
        void getIndividualTemplate_notFound() {
            when(individualTemplateRepo.findByIndividualTemplateIdAndWorkspace_WorkspaceId(99, 1))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getIndividualTemplate(1, 99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 템플릿이 존재하지 않습니다");

            verify(individualTemplateRepo, times(1))
                    .findByIndividualTemplateIdAndWorkspace_WorkspaceId(99, 1);
        }
    }

    @Nested
    @DisplayName("getIndividualTemplateAsync")
    class GetIndividualTemplateAsync {

        @Test
        @DisplayName("@Async로 단건 결과를 CompletableFuture로 반환한다")
        void getIndividualTemplateAsync_success() throws Exception {
            when(workspaceMock.getWorkspaceId()).thenReturn(4);

            LocalDateTime now = LocalDateTime.now();
            IndividualTemplate e = createMockTemplate(44, null, null, null, workspaceMock, now, false);

            when(individualTemplateRepo.findByIndividualTemplateIdAndWorkspace_WorkspaceId(44, 4))
                    .thenReturn(Optional.of(e));

            // when
            CompletableFuture<IndividualTemplateResponse> future =
                    service.getIndividualTemplateAsync(4, 44);

            // then
            IndividualTemplateResponse res = future.get(2, TimeUnit.SECONDS);

            assertThat(res.getIndividualTemplateId()).isEqualTo(44);
            assertThat(res.getWorkspaceId()).isEqualTo(4);
            assertThat(res.getIsDeleted()).isFalse();
            assertThat(res.getCreatedAt()).isEqualTo(now);
            assertThat(res.getUpdatedAt()).isEqualTo(now);

            verify(individualTemplateRepo, times(1))
                    .findByIndividualTemplateIdAndWorkspace_WorkspaceId(44, 4);
        }

        @Test
        @DisplayName("비동기 단건 조회에서도 대상이 없으면 예외가 발생한다")
        void getIndividualTemplateAsync_notFound() {
            when(individualTemplateRepo.findByIndividualTemplateIdAndWorkspace_WorkspaceId(100, 5))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> service.getIndividualTemplateAsync(5, 100))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(individualTemplateRepo, times(1))
                    .findByIndividualTemplateIdAndWorkspace_WorkspaceId(100, 5);
        }
    }

    // Helper
    private IndividualTemplate createMockTemplate(Integer id, String title, String content, String buttonTitle,
                                                  Workspace workspace, LocalDateTime dateTime, boolean isDeleted) {
        IndividualTemplate mockEntity = mock(IndividualTemplate.class);
        when(mockEntity.getIndividualTemplateId()).thenReturn(id);
        when(mockEntity.getIndividualTemplateTitle()).thenReturn(title);
        when(mockEntity.getIndividualTemplateContent()).thenReturn(content);
        when(mockEntity.getButtonTitle()).thenReturn(buttonTitle);
        when(mockEntity.getWorkspace()).thenReturn(workspace);
        when(mockEntity.getCreatedAt()).thenReturn(dateTime);
        when(mockEntity.getUpdatedAt()).thenReturn(dateTime);
        when(mockEntity.getIsDeleted()).thenReturn(isDeleted);
        return mockEntity;
    }
}
