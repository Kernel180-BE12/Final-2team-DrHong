package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.Favorite;
import com.jober.final2teamdrhong.entity.Favorite.TemplateType;
import com.jober.final2teamdrhong.entity.IndividualTemplate;
import com.jober.final2teamdrhong.entity.PublicTemplate;
import com.jober.final2teamdrhong.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Integer>, JpaSpecificationExecutor<Favorite> {

    /**
     * 특정 워크스페이스와 개인 템플릿으로 즐겨찾기 정보를 조회합니다.
     * @param workspace 조회할 워크스페이스 엔티티
     * @param individualTemplate 조회할 개인 템플릿 엔티티
     * @return 존재하면 Optional<Favorite>, 존재하지 않으면 Optional.empty()
     */
    Optional<Favorite> findByWorkspaceAndIndividualTemplate(Workspace workspace, IndividualTemplate individualTemplate);

    /**
     * 특정 워크스페이스와 공용 템플릿으로 즐겨찾기 정보를 조회합니다.
     * @param workspace 조회할 워크스페이스 엔티티
     * @param publicTemplate 조회할 공용 템플릿 엔티티
     * @return 존재하면 Optional<Favorite>, 존재하지 않으면 Optional.empty()
     */
    Optional<Favorite> findByWorkspaceAndPublicTemplate(Workspace workspace, PublicTemplate publicTemplate);

    default void validateIndividualTemplateNotExists(Workspace workspace, IndividualTemplate individualTemplate) {
        findByWorkspaceAndIndividualTemplate(workspace, individualTemplate)
                .ifPresent(f -> {
                    throw new IllegalArgumentException("이미 즐겨찾기된 개인 템플릿입니다.");
                });
    }

    default void validatePublicTemplateNotExists(Workspace workspace, PublicTemplate publicTemplate) {
        findByWorkspaceAndPublicTemplate(workspace, publicTemplate)
                .ifPresent(f -> {
                    throw new IllegalArgumentException("이미 즐겨찾기된 공용 템플릿입니다.");
                });
    }


    /**
     * 특정 워크스페이스에 속한 즐겨찾기 목록을 동적 조건에 따라 페이징하여 조회합니다.
     * @param workspace 조회할 워크스페이스 엔티티
     * @param templateType 템플릿의 유형(PUBLIC 또는 INDIVIDUAL). null일 경우 모든 유형을 조회
     * @param pageable 페이징 및 정렬 정보
     * @return 주어진 조건에 맞는 Favorite 엔티티를 담은 Page 객체
     */
    default Page<Favorite> findFavorites(Workspace workspace, TemplateType templateType, Pageable pageable) {
        Specification<Favorite> spec = hasWorkspace(workspace);

        if (templateType != null) {
            spec = spec.and(isTemplateType(templateType));
        }
        return findAll(spec, pageable);
    }

    private static Specification<Favorite> hasWorkspace(Workspace workspace) {
        return (root, query, cb) -> cb.equal(root.get("workspace"), workspace);
    }

    private static Specification<Favorite> isTemplateType(TemplateType templateType) {
        String fieldName = (templateType == TemplateType.PUBLIC) ? "publicTemplate" : "individualTemplate";
        return (root, query, cb) -> cb.isNotNull(root.get(fieldName));
    }
}
