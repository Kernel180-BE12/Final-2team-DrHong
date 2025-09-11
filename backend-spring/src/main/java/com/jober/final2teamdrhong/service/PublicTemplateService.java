package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.publicTemplate.PublicTemplateResponse;
import com.jober.final2teamdrhong.entity.PublicTemplate;
import com.jober.final2teamdrhong.repository.PublicTemplateRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicTemplateService {

    private final PublicTemplateRepository publicTemplateRepository;

    /**
     * 삭제되지 않은 공용 템플릿 목록을 페이징하여 조회합니다.
     *
     * @param pageable 요청으로부터 바인딩된 페이징/정렬 정보
     * @return 페이징된 PublicTemplateResponse 목록
     */
    @Transactional(readOnly = true)
    public Page<PublicTemplateResponse> getTemplates(Pageable pageable) {
        return publicTemplateRepository.findAllByIsDeletedFalse(pageable)
            .map(this::toResponse);
    }   

    /**
     * PublicTemplate 엔티티를 PublicTemplateResponse DTO로 변환합니다.
     *
     * @param entity 변환할 PublicTemplate 엔티티
     * @return 변환된 PublicTemplateResponse DTO
     */
    private PublicTemplateResponse toResponse(PublicTemplate entity) {
        return new PublicTemplateResponse(
            entity.getPublicTemplateId(),
            entity.getPublicTemplateTitle(),
            entity.getPublicTemplateContent(),
            entity.getButtonTitle()
        );
    }
} 