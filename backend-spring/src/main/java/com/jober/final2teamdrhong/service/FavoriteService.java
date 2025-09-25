package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.favorite.FavoritePageRequest;
import com.jober.final2teamdrhong.dto.favorite.FavoriteResponse;
import com.jober.final2teamdrhong.dto.favorite.IndividualTemplateFavoriteRequest;
import com.jober.final2teamdrhong.dto.favorite.PublicTemplateFavoriteRequest;
import com.jober.final2teamdrhong.entity.Favorite;
import com.jober.final2teamdrhong.entity.Favorite.TemplateType;
import com.jober.final2teamdrhong.entity.IndividualTemplate;
import com.jober.final2teamdrhong.entity.PublicTemplate;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.FavoriteRepository;
import com.jober.final2teamdrhong.repository.IndividualTemplateRepository;
import com.jober.final2teamdrhong.repository.PublicTemplateRepository;
import com.jober.final2teamdrhong.service.validator.WorkspaceValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final WorkspaceValidator workspaceValidator;
    private final IndividualTemplateRepository individualTemplateRepository;
    private final PublicTemplateRepository publicTemplateRepository;

    /**
     * 개인 템플릿을 즐겨찾기에 추가(create)
     * @param request 즐겨찾기 생성을 위한 정보 (workspaceId, templateId)
     * @throws IllegalArgumentException 워크스페이스, 템플릿이 존재하지 않거나 이미 즐겨찾기로 등록되었을 경우 발생
     */
    @Transactional
    public FavoriteResponse createIndividualTemplateFavorite(IndividualTemplateFavoriteRequest request, Integer userId) {
        Workspace workspace = workspaceValidator.validateAndGetWorkspace(request.getWorkspaceId(), userId);

        IndividualTemplate individualTemplate = individualTemplateRepository.findByIdOrThrow(request.getIndividualTemplateId());

        favoriteRepository.validateIndividualTemplateNotExists(workspace, individualTemplate);

        Favorite favorite = Favorite.builder()
                .workspace(workspace)
                .individualTemplate(individualTemplate)
                .build();
        Favorite savedFavorite = favoriteRepository.save(favorite);

        return FavoriteResponse.fromIndividualTemplate(savedFavorite);
    }

    /**
     * 공용 템플릿을 즐겨찾기에 추가(create)
     * @param request 즐겨찾기 생성을 위한 정보 (workspaceId, templateId)
     * @throws IllegalArgumentException 워크스페이스, 템플릿이 존재하지 않거나 이미 즐겨찾기로 등록되었을 경우 발생
     */
    @Transactional
    public FavoriteResponse createPublicTemplateFavorite(PublicTemplateFavoriteRequest request, Integer userId) {
        Workspace workspace = workspaceValidator.validateAndGetWorkspace(request.getWorkspaceId(), userId);

        PublicTemplate publicTemplate = publicTemplateRepository.findByIdOrThrow(request.getPublicTemplateId());

        favoriteRepository.validatePublicTemplateNotExists(workspace, publicTemplate);

        Favorite favorite = Favorite.builder()
                .workspace(workspace)
                .publicTemplate(publicTemplate)
                .build();
        Favorite savedFavorite = favoriteRepository.save(favorite);

        return FavoriteResponse.fromPublicTemplate(savedFavorite);
    }


    // ========== read ==========
    /**
     * 특정 워크스페이스에 속한 즐겨찾기 목록을 조건에 따라 페이징하여 조회(read)합니다.
     * templateType 파라미터가 주어지면 해당 타입의 템플릿만 필터링합니다.
     *
     * @param workspaceId 조회의 기준이 되는 워크스페이스 ID
     * @param templateType 템플릿 유형 (PUBLIC 또는 INDIVIDUAL, optional)
     * @param favoritePageRequest 페이징 정보 (page, size)
     * @return 해당 워크스페이스의 FavoriteResponse DTO 페이지
     * @throws IllegalArgumentException 워크스페이스가 존재하지 않거나 사용자에게 권한이 없을 경우 발생
     */
    public Page<FavoriteResponse> getFavoritesByWorkspace(Integer workspaceId, TemplateType templateType, FavoritePageRequest favoritePageRequest, Integer userId) {
        Workspace workspace = workspaceValidator.validateAndGetWorkspace(workspaceId, userId);
        Pageable pageable = PageRequest.of(favoritePageRequest.getPage(), favoritePageRequest.getSize(), Sort.by(Sort.Direction.DESC, "favoriteId"));

        Page<Favorite> favorites = favoriteRepository.findFavorites(workspace, templateType, pageable);
        return favorites.map(FavoriteResponse::convertToFavoriteResponse);
    }

}
