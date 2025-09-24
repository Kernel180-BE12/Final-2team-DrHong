package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.favorite.FavoritePageRequest;
import com.jober.final2teamdrhong.dto.favorite.FavoriteResponse;
import com.jober.final2teamdrhong.dto.favorite.IndividualTemplateFavoriteRequest;
import com.jober.final2teamdrhong.dto.favorite.PublicTemplateFavoriteRequest;
import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.entity.*;
import com.jober.final2teamdrhong.repository.FavoriteRepository;
import com.jober.final2teamdrhong.repository.IndividualTemplateRepository;
import com.jober.final2teamdrhong.repository.PublicTemplateRepository;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class  FavoriteServiceTest {

    @InjectMocks
    private FavoriteService favoriteService;

    @Mock
    private FavoriteRepository favoriteRepository;
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private IndividualTemplateRepository individualTemplateRepository;
    @Mock
    private PublicTemplateRepository publicTemplateRepository;

    private JwtClaims mockJwtClaims;
    private Workspace mockWorkspace;
    private IndividualTemplate mockIndividualTemplate;
    private PublicTemplate mockPublicTemplate;

    @BeforeEach
    void setUp() {
        mockJwtClaims = JwtClaims.builder().userId(1).build();
        User mockUser = User.builder().userId(1).build();
        mockWorkspace = Workspace.builder()
                .workspaceId(1)
                .user(mockUser)
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl("test-url")
                .representerName("테스트 대표")
                .representerPhoneNumber("010-1234-5678")
                .companyName("테스트 회사")
                .build();
        mockIndividualTemplate = IndividualTemplate.builder().individualTemplateId(10).individualTemplateTitle("개인 템플릿").build();
        mockPublicTemplate = PublicTemplate.builder().publicTemplateId(100).publicTemplateTitle("공용 템플릿").build();
    }

    @Test
    @DisplayName("성공(서비스): 개인 템플릿 즐겨찾기 생성")
    void createIndividualTemplateFavorite_Success() {
        // given
        IndividualTemplateFavoriteRequest request = new IndividualTemplateFavoriteRequest(1, 10);
        Favorite savedFavorite = Favorite.builder().favoriteId(99).workspace(mockWorkspace).individualTemplate(mockIndividualTemplate).build();

        when(workspaceRepository.findByIdOrThrow(1, 1)).thenReturn(mockWorkspace);
        when(individualTemplateRepository.findByIdOrThrow(10)).thenReturn(mockIndividualTemplate);
        doNothing().when(favoriteRepository).validateIndividualTemplateNotExists(mockWorkspace, mockIndividualTemplate);
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(savedFavorite);

        // when
        FavoriteResponse response = favoriteService.createIndividualTemplateFavorite(mockJwtClaims, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFavoriteId()).isEqualTo(99);
        assertThat(response.getTemplateType()).isEqualTo("INDIVIDUAL");
        verify(workspaceRepository).findByIdOrThrow(1, 1);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("실패(서비스): 이미 즐겨찾기한 개인 템플릿 생성 시 예외 발생")
    void createIndividualTemplateFavorite_Fail_AlreadyExists() {
        // given
        IndividualTemplateFavoriteRequest request = new IndividualTemplateFavoriteRequest(1, 10);
        when(workspaceRepository.findByIdOrThrow(1, 1)).thenReturn(mockWorkspace);
        when(individualTemplateRepository.findByIdOrThrow(10)).thenReturn(mockIndividualTemplate);
        doThrow(new IllegalArgumentException("이미 즐겨찾기된 개인 템플릿입니다."))
                .when(favoriteRepository).validateIndividualTemplateNotExists(mockWorkspace, mockIndividualTemplate);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> favoriteService.createIndividualTemplateFavorite(mockJwtClaims, request));
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    @DisplayName("성공(서비스): 공용 템플릿 즐겨찾기 생성")
    void createPublicTemplateFavorite_Success() {
        // given
        PublicTemplateFavoriteRequest request = new PublicTemplateFavoriteRequest(1, 100);
        Favorite savedFavorite = Favorite.builder().favoriteId(101).workspace(mockWorkspace).publicTemplate(mockPublicTemplate).build();

        when(workspaceRepository.findByIdOrThrow(1, 1)).thenReturn(mockWorkspace);
        when(publicTemplateRepository.findByIdOrThrow(100)).thenReturn(mockPublicTemplate);
        doNothing().when(favoriteRepository).validatePublicTemplateNotExists(mockWorkspace, mockPublicTemplate);
        when(favoriteRepository.save(any(Favorite.class))).thenReturn(savedFavorite);

        // when
        FavoriteResponse response = favoriteService.createPublicTemplateFavorite(mockJwtClaims, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getFavoriteId()).isEqualTo(101);
        assertThat(response.getTemplateType()).isEqualTo("PUBLIC");
        verify(workspaceRepository).findByIdOrThrow(1, 1);
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("실패(서비스): 이미 즐겨찾기한 공용 템플릿 생성 시 예외 발생")
    void createPublicTemplateFavorite_Fail_AlreadyExists() {
        // given
        PublicTemplateFavoriteRequest request = new PublicTemplateFavoriteRequest(1, 100);
        when(workspaceRepository.findByIdOrThrow(1, 1)).thenReturn(mockWorkspace);
        when(publicTemplateRepository.findByIdOrThrow(100)).thenReturn(mockPublicTemplate);
        doThrow(new IllegalArgumentException("이미 즐겨찾기된 공용 템플릿입니다."))
                .when(favoriteRepository).validatePublicTemplateNotExists(mockWorkspace, mockPublicTemplate);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> favoriteService.createPublicTemplateFavorite(mockJwtClaims, request));
        verify(favoriteRepository, never()).save(any(Favorite.class));
    }





    // ====================== Read ======================
    @Test
    @DisplayName("성공(서비스): 즐겨찾기 목록 페이징 조회")
    void getFavoritesByWorkspace_Success() {
        // given
        Integer workspaceId = 1;
        FavoritePageRequest pageRequest = new FavoritePageRequest();

        Favorite publicFavorite = Favorite.builder().workspace(mockWorkspace).publicTemplate(mockPublicTemplate).build();
        Page<Favorite> mockPage = new PageImpl<>(List.of(publicFavorite));

        when(workspaceRepository.findByIdOrThrow(workspaceId, mockJwtClaims.getUserId())).thenReturn(mockWorkspace);
        when(favoriteRepository.findFavorites(eq(mockWorkspace), any(), any(Pageable.class))).thenReturn(mockPage);

        // when
        Page<FavoriteResponse> result = favoriteService.getFavoritesByWorkspace(mockJwtClaims, workspaceId, Favorite.TemplateType.PUBLIC, pageRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getTemplateType()).isEqualTo("PUBLIC");
        verify(workspaceRepository).findByIdOrThrow(workspaceId, mockJwtClaims.getUserId());
        verify(favoriteRepository).findFavorites(eq(mockWorkspace), eq(Favorite.TemplateType.PUBLIC), any(Pageable.class));
    }

    @Test
    @DisplayName("실패(서비스): 권한 없는 워크스페이스의 즐겨찾기 조회 시 예외 발생")
    void getFavoritesByWorkspace_Fail_Unauthorized() {
        // given
        Integer workspaceId = 2; // User 1 does not own workspace 2
        FavoritePageRequest pageRequest = new FavoritePageRequest();

        when(workspaceRepository.findByIdOrThrow(workspaceId, mockJwtClaims.getUserId()))
                .thenThrow(new IllegalArgumentException("해당 워크스페이스를 찾을 수 없거나 접근 권한이 없습니다."));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> favoriteService.getFavoritesByWorkspace(mockJwtClaims, workspaceId, null, pageRequest));

        verify(workspaceRepository).findByIdOrThrow(workspaceId, mockJwtClaims.getUserId());
        verify(favoriteRepository, never()).findFavorites(any(), any(), any());
    }
}
