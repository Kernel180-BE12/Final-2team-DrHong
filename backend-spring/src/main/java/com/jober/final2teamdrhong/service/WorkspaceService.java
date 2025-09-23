package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.workspace.WorkspaceRequest;
import com.jober.final2teamdrhong.dto.workspace.WorkspaceResponse;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import com.jober.final2teamdrhong.service.validator.UserValidator;
import com.jober.final2teamdrhong.service.validator.WorkspaceValidator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 워크스페이스 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final UserValidator userValidator;
    private final WorkspaceValidator workspaceValidator;
    private final EntityManager entityManager;

    /**
     * 인증된 사용자를 위해 새로운 워크스페이스를 생성합니다.
     * <p>
     * userId 매개변수를 통해 워크스페이스의 소유자를 지정하며, 이는 생성 권한에 대한 인가(Authorization) 역할을 합니다.
     *
     * @param createDTO 워크스페이스 생성을 위한 요청 데이터
     * @param userId    워크스페이스를 생성할 사용자의 ID (현재 인증된 사용자)
     * @return 생성된 워크스페이스의 간략 정보(SimpleDTO)
     * @throws IllegalArgumentException 해당 ID의 사용자가 존재하지 않거나, 요청된 URL이 이미 사용 중일 경우 발생
     */
    @Transactional
    public WorkspaceResponse.SimpleDTO createWorkspace(WorkspaceRequest.CreateDTO createDTO, Integer userId) {
        // User 조회 및 예외 처리 로직
        User user = userValidator.validateAndGetUser(userId);

        // workspaceUrl unique 조건 확인 로직
        workspaceValidator.validateUrlOnCreate(createDTO.getWorkspaceUrl());

        Workspace workspace = Workspace.builder()
                .workspaceName(createDTO.getWorkspaceName())
                .workspaceSubname(createDTO.getWorkspaceSubname())
                .workspaceAddress(createDTO.getWorkspaceAddress())
                .workspaceDetailAddress(createDTO.getWorkspaceDetailAddress())
                .workspaceUrl(createDTO.getWorkspaceUrl())
                .representerName(createDTO.getRepresenterName())
                .representerPhoneNumber(createDTO.getRepresenterPhoneNumber())
                .representerEmail(createDTO.getRepresenterEmail())
                .companyName(createDTO.getCompanyName())
                .companyRegisterNumber(createDTO.getCompanyRegisterNumber())
                .user(user)
                .build();

        Workspace savedWorkspace = workspaceRepository.save(workspace);

        return new WorkspaceResponse.SimpleDTO(savedWorkspace);
    }

    /**
     * 특정 사용자가 속한 모든 워크스페이스의 목록을 조회합니다.
     * <p>
     * userId를 통해 다른 사용자의 워크스페이스가 조회되지 않도록 인가(Authorization) 처리를 수행합니다.
     *
     * @param userId 조회할 사용자의 ID (현재 인증된 사용자의 ID)
     * @return 해당 사용자의 워크스페이스 간략 정보(SimpleDTO) 리스트
     * @throws IllegalArgumentException 해당 ID의 사용자가 존재하지 않을 경우 발생
     */
    public List<WorkspaceResponse.SimpleDTO> readWorkspaces(Integer userId) {
        // User 조회 및 예외 처리 로직, create와 다르게 존재하는지 확인만 하면 되므로, 'User user =' 는 불필요
        userValidator.validateAndGetUser(userId);

        List<Workspace> workspaceList = workspaceRepository.findAllByUser_UserId(userId);

        return workspaceList.stream()
                .map(WorkspaceResponse.SimpleDTO::new)
                .toList();
    }

    /**
     * 특정 워크스페이스의 상세 정보를 조회합니다.
     * <p>
     * 요청한 사용자가 해당 워크스페이스의 소유자인지 확인하는 인가 과정이 포함됩니다.
     *
     * @param workspaceId 조회할 워크스페이스의 ID
     * @param userId      조회할 사용자의 ID (현재 인증된 사용자의 ID)
     * @return 워크스페이스의 상세 정보(DetailDTO)
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 소유자가 아닐 경우 발생
     */
    public WorkspaceResponse.DetailDTO readWorkspaceDetail(Integer workspaceId, Integer userId) {
        // DB 조회를 한 번으로 최적화하여 워크스페이스 존재 여부와 소유권을 동시에 확인
        // 예외 메세지에, userId는 안넣는 이유:
        // 워크스페이스가 원래부터 없어서 에러가 난 건지 유저에게 권한이 없어서 에러가 난 건지
        // 공격자 입장에서는 이 두 가지 상황을 전혀 구분할 수 없기 때문에 보안상 더 안전함
        Workspace workspaceDetail = workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        // Optional에서 꺼낸 Workspace 엔티티를 DTO 생성자에 전달
        return new WorkspaceResponse.DetailDTO(workspaceDetail);
    }

    /**
     * 특정 워크스페이스의 정보를 수정합니다.
     * <p>
     * 요청한 사용자가 해당 워크스페이스의 소유자인지 확인하는 인가 과정이 포함되며,
     * 수정하려는 워크스페이스 URL의 중복 여부도 검증합니다.
     *
     * @param updateDTO   워크스페이스 수정을 위한 요청 데이터
     * @param workspaceId 수정할 워크스페이스의 ID
     * @param userId      수정을 요청한 사용자의 ID (현재 인증된 사용자)
     * @return 수정된 워크스페이스의 상세 정보(DetailDTO)
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 소유자가 아니거나, 수정하려는
    URL이 이미 사용 중일 경우 발생
     */
    @Transactional
    public WorkspaceResponse.DetailDTO updateWorkspace(WorkspaceRequest.UpdateDTO updateDTO, Integer workspaceId, Integer userId) {
        // 1. 기존 워크스페이스 조회 (소유권 검증 포함)
        Workspace existingWorkspace = workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        // 2. URL 중복 체크 (현재 워크스페이스 제외)
        workspaceValidator.validateUrlOnUpdate(existingWorkspace, updateDTO.getNewWorkspaceUrl());

        // 3. 기존 객체 필드 수정 (Dirty Checking으로 자동 UPDATE)
        existingWorkspace.setWorkspaceName(updateDTO.getNewWorkspaceName());
        existingWorkspace.setWorkspaceSubname(updateDTO.getNewWorkspaceSubname());
        existingWorkspace.setWorkspaceAddress(updateDTO.getNewWorkspaceAddress());
        existingWorkspace.setWorkspaceDetailAddress(updateDTO.getNewWorkspaceDetailAddress());
        existingWorkspace.setWorkspaceUrl(updateDTO.getNewWorkspaceUrl());
        existingWorkspace.setRepresenterName(updateDTO.getNewRepresenterName());
        existingWorkspace.setRepresenterPhoneNumber(updateDTO.getNewRepresenterPhoneNumber());
        existingWorkspace.setRepresenterEmail(updateDTO.getNewRepresenterEmail());
        existingWorkspace.setCompanyName(updateDTO.getNewCompanyName());
        existingWorkspace.setCompanyRegisterNumber(updateDTO.getNewCompanyRegisterNumber());
        existingWorkspace.update();

        return new WorkspaceResponse.DetailDTO(existingWorkspace);
    }

    /**
     * 특정 워크스페이스를 소프트 딜리트 처리합니다.
     * <p>
     * 이 메서드는 {@link Workspace} 엔티티에 적용된 {@code @SQLRestriction("is_deleted = false")}로 인해
     * 일반적인 조회 메서드로는 소프트 딜리트된 엔티티를 재조회할 수 없는 문제를 해결합니다.
     * 정확한 삭제 시간을 응답으로 반환하기 위해 다음의 과정을 거칩니다:
     * <ol>
     *     <li>엔티티의 상태를 변경합니다 (Dirty Checking 대상).</li>
     *     <li>{@code entityManager.flush()}를 호출하여 변경사항을 DB에 즉시 동기화합니다.</li>
     *     <li>{@code entityManager.clear()}를 호출하여 영속성 컨텍스트를 비웁니다.</li>
     *     <li>{@code @SQLRestriction}을 우회하는 네이티브 쿼리 메서드
     *         ({@link WorkspaceRepository#findByIdIncludingDeleted(Integer)})를 사용하여,
     *         DB에 실제로 기록된 최종 상태를 다시 조회합니다.</li>
     *     <li>재조회된 엔티티를 DTO로 변환하여 반환함으로써, 응답 시간과 DB 시간의 일관성을 보장합니다.</li>
     * </ol>
     *
     * @param workspaceId 소프트 딜리트할 워크스페이스의 ID
     * @param userId      요청을 보낸 사용자의 ID (인가에 사용)
     * @return 소프트 딜리트 처리된 워크스페이스의 정보({@link WorkspaceResponse.SimpleDTO})
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 소유자가 아닐 경우 발생
     * @throws IllegalStateException    소프트 딜리트 후 엔티티를 재조회하는 과정에서 문제가 발생했을 경우 발생
     */
    @Transactional
    public WorkspaceResponse.SimpleDTO deleteWorkspace(Integer workspaceId, Integer userId) {
        // 1. 기존 워크스페이스 조회 (소유권 검증 포함)
        Workspace existingWorkspace = workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        // 2. 소프트 딜리트 처리
        existingWorkspace.softDelete();

        // 3. 즉시 DB에 반영, 및 Hibernate 1차 캐시 비우기 후 변경된 DB를 반환해야 정확한 시간이 응답으로 나옴
        entityManager.flush();
        entityManager.clear();

        // 4. @SQLRestriction을 우회하는 네이티브 쿼리로 재조회하여 시간 동기화
        Workspace deletedWorkspace = workspaceRepository.findByIdIncludingDeleted(workspaceId)
                .orElseThrow(() -> new IllegalStateException("소프트 딜리트 처리된 워크스페이스를 재조회하는 데 실패했습니다. ID: " + workspaceId));

        return new WorkspaceResponse.SimpleDTO(deletedWorkspace);
    }
}
