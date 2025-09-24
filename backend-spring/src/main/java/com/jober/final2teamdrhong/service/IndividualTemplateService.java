package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplatePageableRequest;
import com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplateResponse;
import com.jober.final2teamdrhong.entity.IndividualTemplate;
import com.jober.final2teamdrhong.entity.PublicTemplate;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.IndividualTemplateRepository;
import com.jober.final2teamdrhong.repository.PublicTemplateRepository;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import org.springframework.security.access.AccessDeniedException;

import java.util.concurrent.CompletableFuture;

import static com.jober.final2teamdrhong.dto.individualtemplate.IndividualTemplateResponse.toResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndividualTemplateService {

    private final IndividualTemplateRepository individualTemplateRepository;
    private final PublicTemplateRepository publicTemplateRepository;
    private final WorkspaceRepository workspaceRepository;

    /**
     * 비어있는 템플릿 생성 (title/content/button 전부 "")
     * 요청의 문자열 필드는 무시하고 workspaceId만 사용함.
     */
    public void validateWorkspaceOwnership(Integer workspaceId, Integer userId) {
        if (userId == null)
            throw new AccessDeniedException("인증이 필요합니다.");

        boolean exists = workspaceRepository.existsByWorkspaceIdAndUser_UserId(workspaceId, userId);
        if(!exists)
            throw new AccessDeniedException("해당 워크스페이스에 접근 권한이 없습니다.");
    }


    @Transactional
    public IndividualTemplateResponse createTemplate(Integer workspaceId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 workspaceId 입니다. id=" + workspaceId));

        IndividualTemplate entity = IndividualTemplate.builder()
                .workspace(workspace)
                .individualTemplateTitle(null)           // null 저장
                .individualTemplateContent(null)         // null 저장
                .buttonTitle(null)                       // null 저장
                .build();

        IndividualTemplate individualTemplate = individualTemplateRepository.save(entity);

        // save() 직후 createdAt, updatedAt은 Hibernate가 채워주기 때문에 사용 가능
        return toResponse(individualTemplate);
    }

    @Async
    @Transactional
    public CompletableFuture<IndividualTemplateResponse> createTemplateAsync(Integer workspaceId) {
        boolean isVirtual = Thread.currentThread().isVirtual();
        log.info("[@Async] thread={}, isVirtual={}", Thread.currentThread().getName(), isVirtual);

        IndividualTemplateResponse individualTemplateResponse = createTemplate(workspaceId);
        return CompletableFuture.completedFuture(individualTemplateResponse);
    }

    /**
     * 공용 템플릿을 기반으로 개인 템플릿 생성
     */
    @Transactional
    public IndividualTemplateResponse createIndividualTemplateFromPublic(
            Integer publicTemplateId,
            Integer workspaceId,
            Integer userId
    ) {
        // 공용 템플릿 조회
        PublicTemplate publicTemplate = publicTemplateRepository.findByIdOrThrow(publicTemplateId);

        // 워크스페이스 조회
        Workspace workspace = workspaceRepository.findByIdOrThrow(workspaceId, userId);

        // 복사 후 개인 템플릿 생성
        IndividualTemplate newIndividualTemplate = IndividualTemplate.builder()
                .individualTemplateTitle(publicTemplate.getPublicTemplateTitle())
                .individualTemplateContent(publicTemplate.getPublicTemplateContent())
                .buttonTitle(publicTemplate.getButtonTitle())
                .workspace(workspace)
                .build();

        IndividualTemplate individualTemplate = individualTemplateRepository.save(newIndividualTemplate);

        return toResponse(individualTemplate);
    }

    @Async
    @Transactional
    public CompletableFuture<IndividualTemplateResponse> createIndividualTemplateFromPublicAsync(
            Integer publicTemplateId,
            Integer workspaceId,
            Integer userId
    ) {
        boolean isVirtual = Thread.currentThread().isVirtual();
        log.info("[@Async] thread={}, isVirtual={}", Thread.currentThread().getName(), isVirtual);

        IndividualTemplateResponse individualTemplateResponse = createIndividualTemplateFromPublic(
                publicTemplateId, workspaceId, userId);
        return CompletableFuture.completedFuture(individualTemplateResponse);
    }

    /**
     * 개인 템플릿 전체 조회
     */
    @Transactional(readOnly = true)
    public Page<IndividualTemplateResponse> getAllTemplates(
            Integer workspaceId,
            IndividualTemplatePageableRequest pageableRequest) {

        Pageable pageable = pageableRequest.toPageable();

        if (pageableRequest.getStatus() == null) {
            return individualTemplateRepository.findByWorkspace_WorkspaceId(workspaceId, pageable)
                    .map(IndividualTemplateResponse::toResponse);
        } else {
            return individualTemplateRepository.findByWorkspace_WorkspaceIdAndStatus(
                    workspaceId,
                    pageableRequest.getStatus(),
                    pageable)
                    .map(IndividualTemplateResponse::toResponse);
        }
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<IndividualTemplateResponse>> getAllTemplatesAsync(
            Integer workspaceId,
            IndividualTemplatePageableRequest pageableRequest) {
        log.info("[@Async] thread={}, isVirtual={}", Thread.currentThread().getName(), Thread.currentThread().isVirtual());
        return CompletableFuture.completedFuture(getAllTemplates(workspaceId, pageableRequest));
    }

    /**
     * 개인 템플릿 상태별 조회
     */
    @Transactional(readOnly = true)
    public Page<IndividualTemplateResponse> getIndividualTemplateByStatus(
            Integer workspaceId,
            IndividualTemplate.Status status,
            Pageable pageable) {

        return individualTemplateRepository
                .findByWorkspace_WorkspaceIdAndStatus(workspaceId, status, pageable)
                .map(IndividualTemplateResponse::toResponse);
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<Page<IndividualTemplateResponse>> getIndividualTemplateByStatusAsync(
            Integer workspaceId,
            IndividualTemplate.Status status,
            Pageable pageable) {

        log.info("[@Async] thread={}, isVirtual={}", Thread.currentThread().getName(), Thread.currentThread().isVirtual());
        return CompletableFuture.completedFuture(getIndividualTemplateByStatus(workspaceId, status, pageable));
    }

    /**
     * 개인 템플릿 단일 조회
     */
    @Transactional(readOnly = true)
    public IndividualTemplateResponse getIndividualTemplate(Integer workspaceId, Integer individualTemplateId) {
        IndividualTemplate individualTemplate = individualTemplateRepository
                .findByIndividualTemplateIdAndWorkspace_WorkspaceId(individualTemplateId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 템플릿이 존재하지 않습니다. id = " + individualTemplateId));

        return IndividualTemplateResponse.toResponse(individualTemplate);
    }

    @Async
    @Transactional(readOnly = true)
    public CompletableFuture<IndividualTemplateResponse> getIndividualTemplateAsync(Integer workspaceId, Integer individualTemplateId) {
        log.info("[@Async] thread={}, isVirtual={}", Thread.currentThread().getName(), Thread.currentThread().isVirtual());
        return CompletableFuture.completedFuture(getIndividualTemplate(workspaceId, individualTemplateId));
    }
}
