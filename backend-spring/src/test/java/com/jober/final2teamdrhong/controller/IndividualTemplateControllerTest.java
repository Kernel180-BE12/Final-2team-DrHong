package com.jober.final2teamdrhong.controller;

import com.jober.final2teamdrhong.config.ExTestSecurityConfig;
import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplateResponse;
import com.jober.final2teamdrhong.service.IndividualTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

import java.util.concurrent.CompletableFuture;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(controllers = IndividualTemplateController.class)
@Import(ExTestSecurityConfig.class) // 테스트용 시큐리티: permitAll / CSRF off
class IndividualTemplateControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    IndividualTemplateService individualTemplateService;

    @Test
    void createEmptyTemplate_200과_JSON반환() throws Exception {
        // 응답 DTO 인스턴스 생성 (필드 직접 주입)
        IndividualTemplateResponse resp = new IndividualTemplateResponse(); // @NoArgsConstructor 가정
        ReflectionTestUtils.setField(resp, "individualTemplateId", 1);
        ReflectionTestUtils.setField(resp, "workspaceId", 99);

        given(individualTemplateService.createTemplate(99)).willReturn(resp);

        mvc.perform(post("/templates/{workspaceId}", 99))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.individualTemplateId").value(1))
                .andExpect(jsonPath("$.workspaceId").value(99));

        verify(individualTemplateService).createTemplate(99);
    }

    @Test
    void createEmptyTemplateAsync_200과_JSON반환() throws Exception {
        IndividualTemplateResponse resp = new IndividualTemplateResponse();
        ReflectionTestUtils.setField(resp, "individualTemplateId", 2);
        ReflectionTestUtils.setField(resp, "workspaceId", 77);

        given(individualTemplateService.createTemplateAsync(77))
                .willReturn(CompletableFuture.completedFuture(resp));

        // 1차: 비동기 시작 확인
        MvcResult mvcResult = mvc.perform(post("/templates/{workspaceId}/async", 77))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 2차: async 결과 디스패치 후 본검증
        mvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.individualTemplateId").value(2))
                .andExpect(jsonPath("$.workspaceId").value(77));

        verify(individualTemplateService).createTemplateAsync(77);
    }
}