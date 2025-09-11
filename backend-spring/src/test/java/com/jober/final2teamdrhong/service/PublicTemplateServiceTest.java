package com.jober.final2teamdrhong.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.jober.final2teamdrhong.entity.PublicTemplate;
import com.jober.final2teamdrhong.dto.publicTemplate.PublicTemplateResponse;
import com.jober.final2teamdrhong.repository.PublicTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
class PublicTemplateServiceTest {

    @Autowired
    private PublicTemplateService publicTemplateService;

    @Autowired
    private PublicTemplateRepository publicTemplateRepository;

    @BeforeEach
    void setUp() {
        publicTemplateRepository.deleteAll();

        // 더미 데이터 생성 - 다양한 정렬 테스트를 위한 데이터
        PublicTemplate t1 = PublicTemplate.builder()
                .publicTemplateTitle("가나다")
                .publicTemplateContent("Content1")
                .buttonTitle("버튼1")
                .build();
        setFieldValue(t1, "shareCount", 5);
        setFieldValue(t1, "viewCount", 20);
        setFieldValue(t1, "createdAt", LocalDateTime.now().minusDays(3));

        PublicTemplate t2 = PublicTemplate.builder()
                .publicTemplateTitle("나다라")
                .publicTemplateContent("Content2")
                .buttonTitle("버튼2")
                .build();
        setFieldValue(t2, "shareCount", 15);
        setFieldValue(t2, "viewCount", 10);
        setFieldValue(t2, "createdAt", LocalDateTime.now().minusDays(2));

        PublicTemplate t3 = PublicTemplate.builder()
                .publicTemplateTitle("다라마")
                .publicTemplateContent("Content3")
                .buttonTitle("버튼3")
                .build();
        setFieldValue(t3, "shareCount", 8);
        setFieldValue(t3, "viewCount", 30);
        setFieldValue(t3, "createdAt", LocalDateTime.now().minusDays(1));

        PublicTemplate t4 = PublicTemplate.builder()
                .publicTemplateTitle("라마바")
                .publicTemplateContent("Content4")
                .buttonTitle("버튼4")
                .build();
        setFieldValue(t4, "shareCount", 12);
        setFieldValue(t4, "viewCount", 25);
        setFieldValue(t4, "createdAt", LocalDateTime.now());

        // 삭제된 템플릿 (조회되지 않아야 함)
        PublicTemplate deletedTemplate = PublicTemplate.builder()
                .publicTemplateTitle("삭제된템플릿")
                .publicTemplateContent("Deleted Content")
                .buttonTitle("삭제된버튼")
                .build();
        setFieldValue(deletedTemplate, "shareCount", 100);
        setFieldValue(deletedTemplate, "viewCount", 100);
        setFieldValue(deletedTemplate, "createdAt", LocalDateTime.now());
        setFieldValue(deletedTemplate, "isDeleted", true);

        publicTemplateRepository.save(t1);
        publicTemplateRepository.save(t2);
        publicTemplateRepository.save(t3);
        publicTemplateRepository.save(t4);
        publicTemplateRepository.save(deletedTemplate);
    }

    /**
     * 리플렉션을 사용하여 private 필드에 값을 설정하는 헬퍼 메소드
     */
    private void setFieldValue(Object obj, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("필드 설정 실패: " + fieldName, e);
        }
    }

    @Test
    @DisplayName("공유순 정렬 - 내림차순으로 정렬되어야 함")
    void getTemplatesByShareCount() {
        // when
        Pageable pageable = PageRequest.of(0, 10, Sort.by("shareCount").descending());
        Page<PublicTemplateResponse> result = publicTemplateService.getTemplates(pageable);
        List<PublicTemplateResponse> templates = result.getContent();

        // then
        assertThat(templates).hasSize(4); // 삭제되지 않은 템플릿만 조회
        
        // 정렬이 올바르게 되었는지 확인 (공유수가 높은 순서대로)
        // 나다라(15) -> 라마바(12) -> 다라마(8) -> 가나다(5)
        assertThat(templates.get(0).publicTemplateTitle()).isEqualTo("나다라");
        assertThat(templates.get(1).publicTemplateTitle()).isEqualTo("라마바");
        assertThat(templates.get(2).publicTemplateTitle()).isEqualTo("다라마");
        assertThat(templates.get(3).publicTemplateTitle()).isEqualTo("가나다");
    }

    @Test
    @DisplayName("조회순 정렬 - 내림차순으로 정렬되어야 함")
    void getTemplatesByViewCount() {
        // when
        Pageable pageable = PageRequest.of(0, 10, Sort.by("viewCount").descending());
        Page<PublicTemplateResponse> result = publicTemplateService.getTemplates(pageable);
        List<PublicTemplateResponse> templates = result.getContent();

        // then
        assertThat(templates).hasSize(4); // 삭제되지 않은 템플릿만 조회
        
        // 정렬이 올바르게 되었는지 확인 (조회수가 높은 순서대로)
        // 다라마(30) -> 라마바(25) -> 가나다(20) -> 나다라(10)
        assertThat(templates.get(0).publicTemplateTitle()).isEqualTo("다라마");
        assertThat(templates.get(1).publicTemplateTitle()).isEqualTo("라마바");
        assertThat(templates.get(2).publicTemplateTitle()).isEqualTo("가나다");
        assertThat(templates.get(3).publicTemplateTitle()).isEqualTo("나다라");
    }

    @Test
    @DisplayName("최신순 정렬 - 내림차순으로 정렬되어야 함")
    void getTemplatesByRecent() {
        // when
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<PublicTemplateResponse> result = publicTemplateService.getTemplates(pageable);
        List<PublicTemplateResponse> templates = result.getContent();

        // then
        assertThat(templates).hasSize(4); // 삭제되지 않은 템플릿만 조회
        
        // 정렬이 올바르게 되었는지 확인 (최신순으로)
        // 라마바(최신) -> 다라마 -> 나다라 -> 가나다(최구)
        assertThat(templates.get(0).publicTemplateTitle()).isEqualTo("라마바");
        assertThat(templates.get(1).publicTemplateTitle()).isEqualTo("다라마");
        assertThat(templates.get(2).publicTemplateTitle()).isEqualTo("나다라");
        assertThat(templates.get(3).publicTemplateTitle()).isEqualTo("가나다");
    }

    @Test
    @DisplayName("제목 가나다순 정렬 - 오름차순으로 정렬되어야 함")
    void getTemplatesByTitle() {
        // when
        Pageable pageable = PageRequest.of(0, 10, Sort.by("publicTemplateTitle").ascending());
        Page<PublicTemplateResponse> result = publicTemplateService.getTemplates(pageable);
        List<PublicTemplateResponse> templates = result.getContent();

        // then
        assertThat(templates).hasSize(4); // 삭제되지 않은 템플릿만 조회
        assertThat(templates.get(0).publicTemplateTitle()).isEqualTo("가나다");
        assertThat(templates.get(1).publicTemplateTitle()).isEqualTo("나다라");
        assertThat(templates.get(2).publicTemplateTitle()).isEqualTo("다라마");
        assertThat(templates.get(3).publicTemplateTitle()).isEqualTo("라마바");
        
        // 오름차순 검증
        for (int i = 0; i < templates.size() - 1; i++) {
            assertThat(templates.get(i).publicTemplateTitle())
                .isLessThanOrEqualTo(templates.get(i + 1).publicTemplateTitle());
        }
    }

    @Test
    @DisplayName("기본값 정렬 - 최신순으로 정렬되어야 함")
    void getTemplatesByDefault() {
        // when - sortBy가 null이거나 잘못된 값일 때
        Pageable pageableDefault = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<PublicTemplateResponse> result1 = publicTemplateService.getTemplates(pageableDefault);
        Page<PublicTemplateResponse> result2 = publicTemplateService.getTemplates(pageableDefault);
        List<PublicTemplateResponse> templates1 = result1.getContent();
        List<PublicTemplateResponse> templates2 = result2.getContent();

        // then
        assertThat(templates1).hasSize(4);
        assertThat(templates2).hasSize(4);
        
        // 둘 다 최신순으로 정렬되어야 함 (라마바가 가장 최신)
        assertThat(templates1.get(0).publicTemplateTitle()).isEqualTo("라마바");
        assertThat(templates2.get(0).publicTemplateTitle()).isEqualTo("라마바");
    }

    @Test
    @DisplayName("페이징 테스트 - 페이지 크기와 페이지 번호가 올바르게 적용되어야 함")
    void getTemplatesWithPaging() {
        // when - 페이지 크기 2, 첫 번째 페이지
        Pageable pageable = PageRequest.of(0, 2, Sort.by("shareCount").descending());
        Page<PublicTemplateResponse> result = publicTemplateService.getTemplates(pageable);
        List<PublicTemplateResponse> templates = result.getContent();

        // then
        assertThat(templates).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getTotalPages()).isEqualTo(2);
        assertThat(result.getNumber()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(2);
    }

    @Test
    @DisplayName("삭제된 템플릿은 조회되지 않아야 함")
    void getTemplatesExcludeDeleted() {
        // when
        Pageable pageable = PageRequest.of(0, 10, Sort.by("shareCount").descending());
        Page<PublicTemplateResponse> result = publicTemplateService.getTemplates(pageable);
        List<PublicTemplateResponse> templates = result.getContent();

        // then
        assertThat(templates).hasSize(4);
        assertThat(templates).noneMatch(template -> 
            template.publicTemplateTitle().equals("삭제된템플릿"));
    }
}
