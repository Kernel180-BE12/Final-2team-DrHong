package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.workspace.WorkspaceRequest;
import com.jober.final2teamdrhong.dto.workspace.WorkspaceResponse;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import com.jober.final2teamdrhong.service.validator.UserValidator;
import com.jober.final2teamdrhong.service.validator.WorkspaceValidator;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    // @Mock: Mockito가 가짜(Mock) 객체를 생성하여 주입합니다.
    // 이 객체들은 실제 로직을 수행하지 않으며, 오직 정의된 행동(stub)만 수행합니다.
    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private UserValidator userValidator;
    @Mock
    private WorkspaceValidator workspaceValidator;
    @Mock
    private EntityManager entityManager;

    // @InjectMocks: @Mock으로 생성된 가짜 객체들을 실제 테스트 대상인 클래스에 주입합니다.
    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    @DisplayName("워크스페이스 생성 성공 테스트")
    void createWorkspace_Success_Test() {
        // given
        // 1. 테스트에 필요한 변수와 DTO를 정의합니다. (@NonNull 필드 포함)
        Integer userId = 1;
        WorkspaceRequest.CreateDTO createDTO = WorkspaceRequest.CreateDTO.builder()
                .workspaceName("Test Workspace")
                .workspaceUrl("test-url")
                .representerName("Test Rep")
                .representerPhoneNumber("010-1234-5678")
                .companyName("Test Co")
                .build();

        // 2. Validator와 Repository가 반환할 가짜(Mock) 엔티티 객체를 준비합니다.
        User mockUser = mock(User.class);
        Workspace mockWorkspace = mock(Workspace.class);

        // 3. Mockito의 when-thenReturn을 사용하여 의존 객체들의 행동을 정의(stubbing)합니다.
        when(userValidator.validateAndGetUser(userId)).thenReturn(mockUser);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);

        // when
        workspaceService.createWorkspace(createDTO, userId);

        // then
        verify(userValidator, times(1)).validateAndGetUser(userId);
        verify(workspaceValidator, times(1)).validateUrlOnCreate(createDTO.getWorkspaceUrl());
        verify(workspaceRepository, times(1)).save(any(Workspace.class));
    }

    @Test
    @DisplayName("워크스페이스 생성 실패 테스트 - 존재하지 않는 사용자")
    void createWorkspace_Fail_UserNotFound_Test() {
        // given
        Integer userId = 999;
        WorkspaceRequest.CreateDTO createDTO = new WorkspaceRequest.CreateDTO();
        when(userValidator.validateAndGetUser(userId)).thenThrow(new IllegalArgumentException());

        // when
        assertThrows(IllegalArgumentException.class, () -> workspaceService.createWorkspace(createDTO, userId));

        // then
        verify(workspaceValidator, never()).validateUrlOnCreate(anyString());
        verify(workspaceRepository, never()).save(any(Workspace.class));
    }

    @Test
    @DisplayName("워크스페이스 생성 실패 테스트 - 중복된 URL")
    void createWorkspace_Fail_DuplicateUrl_Test() {
        // given
        Integer userId = 1;
        WorkspaceRequest.CreateDTO createDTO = WorkspaceRequest.CreateDTO.builder().workspaceUrl("duplicate-url").build();
        User mockUser = mock(User.class);

        when(userValidator.validateAndGetUser(userId)).thenReturn(mockUser);
        doThrow(new IllegalArgumentException()).when(workspaceValidator).validateUrlOnCreate(createDTO.getWorkspaceUrl());

        // when
        assertThrows(IllegalArgumentException.class, () -> workspaceService.createWorkspace(createDTO, userId));

        // then
        verify(workspaceRepository, never()).save(any(Workspace.class));
    }

    @Test
    @DisplayName("워크스페이스 목록 조회 성공 테스트")
    void readWorkspaces_Success_Test() {
        // given
        Integer userId = 1;
        User mockUser = mock(User.class);
        when(userValidator.validateAndGetUser(userId)).thenReturn(mockUser);
        when(workspaceRepository.findAllByUser_UserId(userId)).thenReturn(List.of());

        // when
        List<WorkspaceResponse.SimpleDTO> result = workspaceService.readWorkspaces(userId);

        // then
        assertNotNull(result);
        verify(userValidator, times(1)).validateAndGetUser(userId);
        verify(workspaceRepository, times(1)).findAllByUser_UserId(userId);
    }

    @Test
    @DisplayName("워크스페이스 목록 조회 실패 테스트 - 존재하지 않는 사용자")
    void readWorkspaces_Fail_UserNotFound_Test() {
        // given
        Integer nonExistingUserId = 999;
        when(userValidator.validateAndGetUser(nonExistingUserId)).thenThrow(new IllegalArgumentException());

        // when
        assertThrows(IllegalArgumentException.class, () -> workspaceService.readWorkspaces(nonExistingUserId));

        // then
        verify(workspaceRepository, never()).findAllByUser_UserId(anyInt());
    }

    @Test
    @DisplayName("워크스페이스 상세 조회 성공 테스트")
    void readWorkspaceDetail_Success_Test() {
        // given
        Integer userId = 1;
        Integer workspaceId = 1;
        Workspace mockWorkspace = mock(Workspace.class);
        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId)).thenReturn(mockWorkspace);

        // when
        WorkspaceResponse.DetailDTO result = workspaceService.readWorkspaceDetail(workspaceId, userId);

        // then
        assertNotNull(result);
        verify(workspaceValidator, times(1)).validateAndGetWorkspace(workspaceId, userId);
    }

    @Test
    @DisplayName("워크스페이스 상세 조회 실패 테스트 - 존재하지 않거나 권한이 없는 워크스페이스")
    void readWorkspaceDetail_Fail_NotFound_Test() {
        // given
        Integer userId = 1;
        Integer nonExistingWorkspaceId = 999;
        when(workspaceValidator.validateAndGetWorkspace(nonExistingWorkspaceId, userId)).thenThrow(new IllegalArgumentException());

        // when
        assertThrows(IllegalArgumentException.class, () -> workspaceService.readWorkspaceDetail(nonExistingWorkspaceId, userId));

        // then: 이 시나리오에서는 후속 검증이 필요 없습니다.
    }

    @Test
    @DisplayName("워크스페이스 수정 성공 테스트")
    void updateWorkspace_Success_Test() {
        // given
        Integer userId = 1;
        Integer workspaceId = 1;
        // UpdateDTO 생성 시, NonNull 필드에 해당하는 값을 모두 설정
        WorkspaceRequest.UpdateDTO updateDTO = WorkspaceRequest.UpdateDTO.builder()
                .newWorkspaceName("Updated Workspace")
                .newWorkspaceUrl("updated-url")
                .newRepresenterName("Updated Rep")
                .newRepresenterPhoneNumber("010-8765-4321")
                .newCompanyName("Updated Co")
                .build();
        User mockUser = mock(User.class);

        // Spy 객체 생성 시 @NonNull 필드를 모두 포함하여 NullPointerException 방지
        Workspace existingWorkspace = spy(Workspace.builder()
                .workspaceName("Original Workspace")
                .workspaceUrl("original-url")
                .representerName("Original Rep")
                .representerPhoneNumber("010-1234-5678")
                .companyName("Original Co")
                .user(mockUser)
                .build());

        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId)).thenReturn(existingWorkspace);

        // when
        workspaceService.updateWorkspace(updateDTO, workspaceId, userId);

        // then
        verify(workspaceValidator, times(1)).validateAndGetWorkspace(workspaceId, userId);
        verify(workspaceValidator, times(1)).validateUrlOnUpdate(existingWorkspace, updateDTO.getNewWorkspaceUrl());
        verify(existingWorkspace, times(1)).update();
    }

    @Test
    @DisplayName("워크스페이스 수정 실패 테스트 - 존재하지 않거나 권한이 없는 워크스페이스")
    void updateWorkspace_Fail_NotFoundOrUnauthorized_Test() {
        // given
        Integer userId = 1;
        Integer nonExistingWorkspaceId = 999;
        WorkspaceRequest.UpdateDTO updateDTO = new WorkspaceRequest.UpdateDTO();
        when(workspaceValidator.validateAndGetWorkspace(nonExistingWorkspaceId, userId)).thenThrow(new IllegalArgumentException());

        // when
        assertThrows(IllegalArgumentException.class, () -> workspaceService.updateWorkspace(updateDTO, nonExistingWorkspaceId, userId));

        // then
        verify(workspaceValidator, never()).validateUrlOnUpdate(any(), any());
    }

    @Test
    @DisplayName("워크스페이스 수정 실패 테스트 - 중복된 URL")
    void updateWorkspace_Fail_DuplicateUrl_Test() {
        // given
        Integer userId = 1;
        Integer workspaceId = 1;
        WorkspaceRequest.UpdateDTO updateDTO = WorkspaceRequest.UpdateDTO.builder().newWorkspaceUrl("duplicate-url").build();
        Workspace existingWorkspace = mock(Workspace.class);

        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId)).thenReturn(existingWorkspace);
        doThrow(new IllegalArgumentException()).when(workspaceValidator).validateUrlOnUpdate(existingWorkspace, updateDTO.getNewWorkspaceUrl());

        // when
        assertThrows(IllegalArgumentException.class, () -> workspaceService.updateWorkspace(updateDTO, workspaceId, userId));

        // then: 이 시나리오에서는 후속 검증이 필요 없습니다.
    }

    @Test
    @DisplayName("워크스페이스 삭제 성공 테스트")
    void deleteWorkspace_Success_Test() {
        // given
        Integer userId = 1;
        Integer workspaceId = 1;
        User mockUser = mock(User.class);

        // Spy 객체 생성 시 @NonNull 필드를 모두 포함하여 NullPointerException 방지
        Workspace existingWorkspace = spy(Workspace.builder()
                .workspaceName("Workspace to Delete")
                .workspaceUrl("delete-url")
                .representerName("Delete Rep")
                .representerPhoneNumber("010-1234-5678")
                .companyName("Delete Co")
                .user(mockUser)
                .build());

        // 소프트 딜리트 후 재조회될 워크스페이스 Mock 객체
        Workspace deletedWorkspace = mock(Workspace.class);

        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId)).thenReturn(existingWorkspace);
        when(workspaceRepository.findByIdIncludingDeleted(workspaceId)).thenReturn(Optional.of(deletedWorkspace));

        // when
        WorkspaceResponse.SimpleDTO result = workspaceService.deleteWorkspace(workspaceId, userId);

        // then
        assertNotNull(result);
        verify(workspaceValidator, times(1)).validateAndGetWorkspace(workspaceId, userId);
        verify(existingWorkspace, times(1)).softDelete();
        verify(entityManager, times(1)).flush();
        verify(entityManager, times(1)).clear();
        verify(workspaceRepository, times(1)).findByIdIncludingDeleted(workspaceId);
    }

    @Test
    @DisplayName("워크스페이스 삭제 실패 테스트 - 존재하지 않거나 권한이 없는 워크스페이스")
    void deleteWorkspace_Fail_NotFoundOrUnauthorized_Test() {
        // given
        Integer userId = 1;
        Integer nonExistingWorkspaceId = 999;
        when(workspaceValidator.validateAndGetWorkspace(nonExistingWorkspaceId, userId)).thenThrow(new IllegalArgumentException());

        // when
        assertThrows(IllegalArgumentException.class, () -> workspaceService.deleteWorkspace(nonExistingWorkspaceId, userId));

        // then: 이 시나리오에서는 후속 검증이 필요 없습니다.
    }

    @Test
    @DisplayName("워크스페이스 삭제 실패 테스트 - 소프트 딜리트 후 재조회 실패")
    void deleteWorkspace_Fail_RefetchFailed_Test() {
        // given
        Integer userId = 1;
        Integer workspaceId = 1;
        User mockUser = mock(User.class);

        Workspace existingWorkspace = spy(Workspace.builder()
                .workspaceName("Workspace to Delete")
                .workspaceUrl("delete-url")
                .representerName("Delete Rep")
                .representerPhoneNumber("010-1234-5678")
                .companyName("Delete Co")
                .user(mockUser)
                .build());

        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId)).thenReturn(existingWorkspace);
        when(workspaceRepository.findByIdIncludingDeleted(workspaceId)).thenReturn(Optional.empty());

        // when & then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> workspaceService.deleteWorkspace(workspaceId, userId));

        assertThat(exception.getMessage()).isEqualTo("소프트 딜리트 처리된 워크스페이스를 재조회하는 데 실패했습니다. ID: " + workspaceId);
        verify(workspaceValidator, times(1)).validateAndGetWorkspace(workspaceId, userId);
        verify(existingWorkspace, times(1)).softDelete();
        verify(entityManager, times(1)).flush();
        verify(entityManager, times(1)).clear();
        verify(workspaceRepository, times(1)).findByIdIncludingDeleted(workspaceId);
    }
}