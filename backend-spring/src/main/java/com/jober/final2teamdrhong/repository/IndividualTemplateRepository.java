package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.IndividualTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IndividualTemplateRepository extends JpaRepository<IndividualTemplate, Integer> {
    default IndividualTemplate findByIdOrThrow(Integer templateId) {
        return findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("해당 개인 템플릿을 찾을 수 없습니다."));
    }

    // 개인 템플릿 목록 전체 조회
    Page<IndividualTemplate> findByWorkspace_WorkspaceId(Integer workspaceId, Pageable pageable);

    // 개인 템플릿 목록 상태별 조회
    Page<IndividualTemplate> findByWorkspace_WorkspaceIdAndStatus(
            Integer workspaceId,
            IndividualTemplate.Status status,
            Pageable pageable
    );

    // 개인 템플릿 단일 조회
    Optional<IndividualTemplate> findByIndividualTemplateIdAndWorkspace_WorkspaceId(Integer individualTemplateId, Integer workspaceId);
}
