package com.jober.final2teamdrhong.service.validator;

import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceValidatorTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @InjectMocks
    private WorkspaceValidator workspaceValidator;

    @Test
    @DisplayName("워크스페이스 검증 및 조회 성공 테스트")
    void validateAndGetWorkspace_Success_Test() {
        // given
        // 1. 테스트용 워크스페이스 ID와 사용자 ID를 정의합니다.
        Integer workspaceId = 1;
        Integer userId = 1;
        // 2. Mock Repository가 반환할 워크스페이스 엔티티를 생성합니다.
        User mockUser = mock(User.class);
        Workspace mockWorkspace = Workspace.builder()
                .workspaceId(workspaceId)
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl("테스트 워크스페이스")
                .representerName("테스트 워크스페이스")
                .representerPhoneNumber("테스트 워크스페이스")
                .companyName("테스트 워크스페이스")
                .user(mockUser)
                .build();

        // 3. Mock Repository의 동작을 정의합니다: ID로 조회 시 mockWorkspace를 반환하도록 설정합니다.
        when(workspaceRepository.findByWorkspaceIdAndUser_UserId(workspaceId, userId))
                .thenReturn(Optional.of(mockWorkspace));

        // when
        // 1. 테스트 대상인 validator 메서드를 호출합니다.
        Workspace result = workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        // then
        // 1. 반환된 결과가 null이 아닌지 확인합니다.
        assertThat(result).isNotNull();
        // 2. 반환된 워크스페이스의 ID가 예상과 일치하는지 확인합니다.
        assertThat(result.getWorkspaceId()).isEqualTo(workspaceId);
        // 3. 반환된 워크스페이스의 이름이 예상과 일치하는지 확인합니다.
        assertThat(result.getWorkspaceName()).isEqualTo("테스트 워크스페이스");
        // 4. 반환된 워크스페이스의 소유자가 예상과 일치하는지 확인합니다.
        assertThat(result.getUser()).isEqualTo(mockUser);
    }

    @Test
    @DisplayName("워크스페이스 검증 및 조회 실패 테스트 - 워크스페이스가 존재하지 않거나 접근 권한 없음")
    void validateAndGetWorkspace_Fail_NotFoundOrNoAccess_Test() {
        // given
        // 1. 테스트용 워크스페이스 ID와 사용자 ID를 정의합니다.
        Integer workspaceId = 999;
        Integer userId = 1;

        // 2. Mock Repository의 동작을 정의합니다: 조회 시 빈 Optional을 반환하도록 설정합니다.
        when(workspaceRepository.findByWorkspaceIdAndUser_UserId(workspaceId, userId))
                .thenReturn(Optional.empty());

        // when
        // 1. 예외를 발생시킬 행위를 실행하고, 발생한 예외를 `thrown` 객체에 담습니다.
        Throwable thrown = catchThrowable(() -> workspaceValidator.validateAndGetWorkspace(workspaceId, userId));

        // then
        // 1. 발생한 예외가 `IllegalArgumentException` 타입인지 확인합니다.
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        // 2. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertThat(thrown.getMessage()).isEqualTo("워크스페이스를 찾을 수 없거나 접근권한이 없습니다. ID: " + workspaceId);
    }

    @Test
    @DisplayName("워크스페이스 생성 시 URL 중복 검증 성공 테스트 - URL이 중복되지 않음")
    void validateUrlOnCreate_Success_Test() {
        // given
        // 1. 중복되지 않는 새로운 워크스페이스 URL을 정의합니다.
        String newWorkspaceUrl = "new-workspace-url";

        // 2. Mock Repository의 동작을 정의합니다: URL 존재 여부 확인 시 false를 반환하도록 설정합니다.
        when(workspaceRepository.existsByWorkspaceUrl(newWorkspaceUrl)).thenReturn(false);

        // when
        // 1. 예외를 발생시키지 않을 행위를 실행하고, 결과를 `thrown` 객체에 담습니다.
        Throwable thrown = catchThrowable(() -> workspaceValidator.validateUrlOnCreate(newWorkspaceUrl));

        // then
        // 1. 아무 예외도 발생하지 않았는지(thrown 객체가 null인지) 확인합니다.
        assertThat(thrown).isNull();
    }

    @Test
    @DisplayName("워크스페이스 생성 시 URL 중복 검증 실패 테스트 - URL이 이미 존재함")
    void validateUrlOnCreate_Fail_UrlAlreadyExists_Test() {
        // given
        // 1. 이미 존재하는 워크스페이스 URL을 정의합니다.
        String existingWorkspaceUrl = "existing-workspace-url";

        // 2. Mock Repository의 동작을 정의합니다: URL 존재 여부 확인 시 true를 반환하도록 설정합니다.
        when(workspaceRepository.existsByWorkspaceUrl(existingWorkspaceUrl)).thenReturn(true);

        // when
        // 1. 예외를 발생시킬 행위를 실행하고, 발생한 예외를 `thrown` 객체에 담습니다.
        Throwable thrown = catchThrowable(() -> workspaceValidator.validateUrlOnCreate(existingWorkspaceUrl));

        // then
        // 1. 발생한 예외가 `IllegalArgumentException` 타입인지 확인합니다.
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        // 2. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertThat(thrown.getMessage()).isEqualTo("이미 사용 중인 URL입니다. 다른 URL을 입력해주세요.");
    }

    @Test
    @DisplayName("워크스페이스 수정 시 URL 중복 검증 성공 테스트 - URL이 변경되지 않음")
    void validateUrlOnUpdate_Success_UrlNotChanged_Test() {
        // given
        // 1. 기존 워크스페이스와 동일한 URL을 정의합니다.
        String sameUrl = "same-url";
        User mockUser = mock(User.class);
        Workspace existingWorkspace = Workspace.builder()
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl(sameUrl)
                .representerName("테스트 워크스페이스")
                .representerPhoneNumber("테스트 워크스페이스")
                .companyName("테스트 워크스페이스")
                .user(mockUser)
                .build();

        // when
        // 1. URL이 변경되지 않은 시나리오에서 validator 메서드를 호출합니다.
        // 2. 이 경우 repository의 existsByWorkspaceUrl는 호출되지 않아야 합니다.
        Throwable thrown = catchThrowable(() -> workspaceValidator.validateUrlOnUpdate(existingWorkspace, sameUrl));

        // then
        // 1. 아무 예외도 발생하지 않았는지 확인합니다.
        assertThat(thrown).isNull();
    }

    @Test
    @DisplayName("워크스페이스 수정 시 URL 중복 검증 성공 테스트 - URL이 중복되지 않는 새 URL로 변경됨")
    void validateUrlOnUpdate_Success_UrlChangedToUnique_Test() {
        // given
        // 1. 기존 워크스페이스와 다른, 중복되지 않는 새 URL을 정의합니다.
        String originalUrl = "original-url";
        String newUniqueUrl = "new-unique-url";
        User mockUser = mock(User.class);
        Workspace existingWorkspace = Workspace.builder()
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl(originalUrl)
                .representerName("테스트 워크스페이스")
                .representerPhoneNumber("테스트 워크스페이스")
                .companyName("테스트 워크스페이스")
                .user(mockUser)
                .build();

        // 2. Mock Repository의 동작을 정의합니다: 새 URL 존재 여부 확인 시 false를 반환하도록 설정합니다.
        when(workspaceRepository.existsByWorkspaceUrl(newUniqueUrl)).thenReturn(false);

        // when
        // 1. 예외를 발생시키지 않을 행위를 실행하고, 결과를 `thrown` 객체에 담습니다.
        Throwable thrown = catchThrowable(() -> workspaceValidator.validateUrlOnUpdate(existingWorkspace, newUniqueUrl));

        // then
        // 1. 아무 예외도 발생하지 않았는지 확인합니다.
        assertThat(thrown).isNull();
    }

    @Test
    @DisplayName("워크스페이스 수정 시 URL 중복 검증 실패 테스트 - URL이 이미 존재하는 URL로 변경됨")
    void validateUrlOnUpdate_Fail_UrlChangedToExisting_Test() {
        // given
        // 1. 기존 워크스페이스와 다른, 하지만 이미 존재하는 URL을 정의합니다.
        String originalUrl = "original-url";
        String newExistingUrl = "new-existing-url";
        User mockUser = mock(User.class);
        Workspace existingWorkspace = Workspace.builder()
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl(originalUrl)
                .representerName("테스트 워크스페이스")
                .representerPhoneNumber("테스트 워크스페이스")
                .companyName("테스트 워크스페이스")
                .user(mockUser)
                .build();

        // 2. Mock Repository의 동작을 정의합니다: 새 URL 존재 여부 확인 시 true를 반환하도록 설정합니다.
        when(workspaceRepository.existsByWorkspaceUrl(newExistingUrl)).thenReturn(true);

        // when
        // 1. 예외를 발생시킬 행위를 실행하고, 발생한 예외를 `thrown` 객체에 담습니다.
        Throwable thrown = catchThrowable(() -> workspaceValidator.validateUrlOnUpdate(existingWorkspace, newExistingUrl));

        // then
        // 1. 발생한 예외가 `IllegalArgumentException` 타입인지 확인합니다.
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class);
        // 2. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertThat(thrown.getMessage()).isEqualTo("이미 사용 중인 URL입니다. 다른 URL을 입력해주세요.");
    }
}
