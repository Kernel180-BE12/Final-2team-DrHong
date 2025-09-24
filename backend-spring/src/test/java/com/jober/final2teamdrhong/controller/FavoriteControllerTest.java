package com.jober.final2teamdrhong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jober.final2teamdrhong.dto.favorite.FavoritePageRequest;
import com.jober.final2teamdrhong.dto.favorite.FavoriteResponse;
import com.jober.final2teamdrhong.dto.favorite.IndividualTemplateFavoriteRequest;
import com.jober.final2teamdrhong.dto.favorite.PublicTemplateFavoriteRequest;
import com.jober.final2teamdrhong.dto.jwtClaims.JwtClaims;
import com.jober.final2teamdrhong.filter.JwtAuthenticationFilter;
import com.jober.final2teamdrhong.service.FavoriteService;
import com.jober.final2teamdrhong.util.test.WithMockJwtClaims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc(addFilters = false)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FavoriteService favoriteService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean(name = "jpaMappingContext")
    private MappingContext<?, ?> jpaMappingContext;

    @Test
    @DisplayName("성공(단위): 개인 템플릿 즐겨찾기 생성")
    @WithMockJwtClaims(userId = 1)
    void createIndividualTemplateFavorite_Success() throws Exception {
        // given
        IndividualTemplateFavoriteRequest request = new IndividualTemplateFavoriteRequest(1, 10);
        String requestBody = objectMapper.writeValueAsString(request);

        FavoriteResponse response = FavoriteResponse.builder()
                .favoriteId(1)
                .templateType("INDIVIDUAL")
                .templateId(10)
                .templateTitle("테스트 개인 템플릿")
                .build();

        when(favoriteService.createIndividualTemplateFavorite(any(JwtClaims.class), any(IndividualTemplateFavoriteRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/individual/favorite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.favoriteId").value(1))
                .andExpect(jsonPath("$.templateType").value("INDIVIDUAL"))
                .andExpect(jsonPath("$.templateId").value(10));

        verify(favoriteService).createIndividualTemplateFavorite(any(JwtClaims.class), any(IndividualTemplateFavoriteRequest.class));
    }

    @Test
    @DisplayName("성공(단위): 공용 템플릿 즐겨찾기 생성")
    @WithMockJwtClaims(userId = 1)
    void createPublicTemplateFavorite_Success() throws Exception {
        // given
        PublicTemplateFavoriteRequest request = new PublicTemplateFavoriteRequest(1, 100);
        String requestBody = objectMapper.writeValueAsString(request);

        FavoriteResponse response = FavoriteResponse.builder()
                .favoriteId(2)
                .templateType("PUBLIC")
                .templateId(100)
                .templateTitle("테스트 공용 템플릿")
                .build();

        when(favoriteService.createPublicTemplateFavorite(any(JwtClaims.class), any(PublicTemplateFavoriteRequest.class)))
                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/public/favorite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.favoriteId").value(2))
                .andExpect(jsonPath("$.templateType").value("PUBLIC"))
                .andExpect(jsonPath("$.templateId").value(100));

        verify(favoriteService).createPublicTemplateFavorite(any(JwtClaims.class), any(PublicTemplateFavoriteRequest.class));
    }





    // ====================== Read ======================
    @Test
    @DisplayName("성공(컨트롤러): 즐겨찾기 목록 페이징 조회")
    @WithMockJwtClaims // userId = 1
    void getFavoritesByWorkspace_Success() throws Exception {
        // given
        Integer workspaceId = 1;
        Page<FavoriteResponse> mockPage = new PageImpl<>(List.of(
                FavoriteResponse.builder().favoriteId(1).templateType("PUBLIC").build()
        ));
        when(favoriteService.getFavoritesByWorkspace(any(JwtClaims.class), eq(workspaceId), any(), any(FavoritePageRequest.class)))
                .thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/favorites")
                        .param("workspaceId", String.valueOf(workspaceId))
                        .param("templateType", "PUBLIC")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].templateType").value("PUBLIC"));

        verify(favoriteService).getFavoritesByWorkspace(any(JwtClaims.class), eq(workspaceId), any(), any(FavoritePageRequest.class));
    }

    @Test
    @DisplayName("실패(컨트롤러): 권한 없는 워크스페이스 조회 시 400 응답")
    @WithMockJwtClaims
    void getFavoritesByWorkspace_Fail_Unauthorized() throws Exception {
        // given
        Integer workspaceId = 2;
        String errorMessage = "해당 워크스페이스를 찾을 수 없거나 접근 권한이 없습니다.";
        when(favoriteService.getFavoritesByWorkspace(any(JwtClaims.class), eq(workspaceId), any(), any(FavoritePageRequest.class)))
                .thenThrow(new IllegalArgumentException(errorMessage));

        // when & then
        mockMvc.perform(get("/favorites")
                        .param("workspaceId", String.valueOf(workspaceId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errorMessage));

        verify(favoriteService).getFavoritesByWorkspace(any(JwtClaims.class), eq(workspaceId), any(), any(FavoritePageRequest.class));
    }
}
