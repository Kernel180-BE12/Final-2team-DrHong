package com.jober.final2teamdrhong.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jober.final2teamdrhong.dto.publicTemplate.PublicTemplateResponse;
import com.jober.final2teamdrhong.service.PublicTemplateService;

@WebMvcTest(PublicTemplateController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicTemplateControllerTest {
    
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PublicTemplateService publicTemplateService;

    @Test
    @DisplayName("기본 파라미터로 템플릿 조회 - 최신순 정렬")
    void getPublicTemplates_WithDefaultParams_ReturnsRecentSorted() throws Exception {
        // given
        List<PublicTemplateResponse> mockContent = List.of(
            createMockResponse(1, "최신템플릿", "Content1"),
            createMockResponse(2, "오래된템플릿", "Content2")
        );
        Page<PublicTemplateResponse> mockPage = new PageImpl<>(mockContent, PageRequest.of(0, 10), 2);
        
        when(publicTemplateService.getTemplates(any(Pageable.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/public-templates")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].publicTemplateTitle").value("최신템플릿"))
                .andExpect(jsonPath("$.content[1].publicTemplateTitle").value("오래된템플릿"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(publicTemplateService, times(1)).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("공유순 정렬로 템플릿 조회")
    void getPublicTemplates_WithShareSort_ReturnsShareSorted() throws Exception {
        // given
        List<PublicTemplateResponse> mockContent = List.of(
            createMockResponse(1, "높은공유템플릿", "Content1"),
            createMockResponse(2, "낮은공유템플릿", "Content2")
        );
        Page<PublicTemplateResponse> mockPage = new PageImpl<>(mockContent, PageRequest.of(0, 10), 2);
        
        when(publicTemplateService.getTemplates(any(Pageable.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/public-templates")
                        .param("sort", "shareCount")
                        .param("direction", "DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].publicTemplateTitle").value("높은공유템플릿"))
                .andExpect(jsonPath("$.content[1].publicTemplateTitle").value("낮은공유템플릿"));

        verify(publicTemplateService, times(1)).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("조회순 정렬로 템플릿 조회")
    void getPublicTemplates_WithViewSort_ReturnsViewSorted() throws Exception {
        // given
        List<PublicTemplateResponse> mockContent = List.of(
            createMockResponse(1, "높은조회템플릿", "Content1"),
            createMockResponse(2, "낮은조회템플릿", "Content2")
        );
        Page<PublicTemplateResponse> mockPage = new PageImpl<>(mockContent, PageRequest.of(0, 10), 2);
        
        when(publicTemplateService.getTemplates(any(Pageable.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/public-templates")
                        .param("sort", "viewCount")
                        .param("direction", "DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].publicTemplateTitle").value("높은조회템플릿"))
                .andExpect(jsonPath("$.content[1].publicTemplateTitle").value("낮은조회템플릿"));

        verify(publicTemplateService, times(1)).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("제목순 정렬로 템플릿 조회")
    void getPublicTemplates_WithTitleSort_ReturnsTitleSorted() throws Exception {
        // given
        List<PublicTemplateResponse> mockContent = List.of(
            createMockResponse(1, "가나다", "Content1"),
            createMockResponse(2, "나다라", "Content2")
        );
        Page<PublicTemplateResponse> mockPage = new PageImpl<>(mockContent, PageRequest.of(0, 10), 2);
        
        when(publicTemplateService.getTemplates(any(Pageable.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/public-templates")
                        .param("sort", "publicTemplateTitle")
                        .param("direction", "ASC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].publicTemplateTitle").value("가나다"))
                .andExpect(jsonPath("$.content[1].publicTemplateTitle").value("나다라"));

        verify(publicTemplateService, times(1)).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("페이징 파라미터로 템플릿 조회")
    void getPublicTemplates_WithPagingParams_ReturnsPagedResult() throws Exception {
        // given
        List<PublicTemplateResponse> mockContent = List.of(
            createMockResponse(1, "템플릿1", "Content1")
        );
        Page<PublicTemplateResponse> mockPage = new PageImpl<>(mockContent, PageRequest.of(1, 2), 5);
        
        when(publicTemplateService.getTemplates(any(Pageable.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/public-templates")
                        .param("page", "1")
                        .param("size", "2")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(1))
                .andExpect(jsonPath("$.size").value(2));

        verify(publicTemplateService, times(1)).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("잘못된 정렬 옵션으로 템플릿 조회 - 400 에러 반환")
    void getPublicTemplates_WithInvalidSort_ReturnsBadRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/public-templates")
                        .param("sort", "invalid")
                        .param("direction", "DESC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(publicTemplateService, never()).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("잘못된 정렬 방향으로 템플릿 조회 - 400 에러 반환")
    void getPublicTemplates_WithInvalidDirection_ReturnsBadRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/public-templates")
                        .param("sort", "createdAt")
                        .param("direction", "INVALID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(publicTemplateService, never()).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("잘못된 페이지 번호로 템플릿 조회 - 400 에러 반환")
    void getPublicTemplates_WithInvalidPage_ReturnsBadRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/public-templates")
                        .param("page", "-1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(publicTemplateService, never()).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("잘못된 페이지 크기로 템플릿 조회 - 400 에러 반환")
    void getPublicTemplates_WithInvalidSize_ReturnsBadRequest() throws Exception {
        // when & then
        mockMvc.perform(get("/public-templates")
                        .param("size", "0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(publicTemplateService, never()).getTemplates(any(Pageable.class));
    }

    @Test
    @DisplayName("빈 결과 조회")
    void getPublicTemplates_WithEmptyResult_ReturnsEmptyPage() throws Exception {
        // given
        Page<PublicTemplateResponse> mockPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        
        when(publicTemplateService.getTemplates(any(Pageable.class))).thenReturn(mockPage);

        // when & then
        mockMvc.perform(get("/public-templates")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));

        verify(publicTemplateService, times(1)).getTemplates(any(Pageable.class));
    }

    private PublicTemplateResponse createMockResponse(Integer id, String title, String content) {
        return new PublicTemplateResponse(
                id,
                title,
                content,
                null
        );
    }
}
