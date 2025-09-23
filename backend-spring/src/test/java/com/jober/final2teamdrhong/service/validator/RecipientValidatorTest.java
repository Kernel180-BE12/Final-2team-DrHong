package com.jober.final2teamdrhong.service.validator;

import com.jober.final2teamdrhong.entity.Recipient;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.RecipientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipientValidatorTest {

    @Mock
    private RecipientRepository recipientRepository;

    @InjectMocks
    private RecipientValidator recipientValidator;

    @Test
    @DisplayName("수신자 단건 검증 및 조회 성공 테스트")
    void validateAndGetRecipient_Success_Test() {
        // given
        // 1. 테스트용 워크스페이스 ID와 검증할 수신자 ID를 정의합니다.
        Integer workspaceId = 1;
        Integer recipientId = 1;
        // 2. Mock Repository가 반환할 수신자 엔티티를 생성합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        Recipient mockRecipient = Recipient.builder()
                .recipientId(1)
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1111-1111")
                .workspace(mockWorkspace)
                .build();

        // 3. Mock Repository의 동작을 정의합니다: ID로 조회 시 mockRecipient를 반환하도록 설정합니다.
        when(recipientRepository.findByRecipientIdAndWorkspace_WorkspaceId(recipientId, workspaceId))
                .thenReturn(Optional.of(mockRecipient));

        // when
        // 1. 테스트 대상인 validator 메서드를 호출합니다.
        Recipient result = recipientValidator.validateAndGetRecipient(workspaceId, recipientId);

        // then
        // 1. 반환된 결과가 null이 아닌지 확인합니다.
        assertThat(result).isNotNull();
        // 2. 반환된 수신자의 ID가 예상과 일치하는지 확인합니다.
        assertThat(result.getRecipientId()).isEqualTo(recipientId);
        // 3. 반환된 수신자의 이름이 예상과 일치하는지 확인합니다.
        assertThat(result.getRecipientName()).isEqualTo("홍길동");
        // 4. 반환된 수신자가 속한 워크스페이스가 예상과 일치하는지 확인합니다.
        assertThat(result.getWorkspace()).isEqualTo(mockWorkspace);
    }

    @Test
    @DisplayName("수신자 단건 검증 및 조회 실패 테스트 - 수신자가 존재하지 않음")
    void validateAndGetRecipient_Fail_RecipientNotFound_Test() {
        // given
        // 1. 테스트용 워크스페이스 ID와 존재하지 않는 수신자 ID를 정의합니다.
        Integer workspaceId = 1;
        Integer recipientId = 999;

        // 2. Mock Repository의 동작을 정의합니다: 조회 시 빈 Optional을 반환하도록 설정합니다.
        when(recipientRepository.findByRecipientIdAndWorkspace_WorkspaceId(recipientId, workspaceId))
                .thenReturn(Optional.empty());

        // when & then
        // 1. validator 메서드 호출 시 IllegalArgumentException이 발생하는 것을 검증합니다.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> recipientValidator.validateAndGetRecipient(workspaceId, recipientId));

        // 2. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertThat(exception.getMessage()).isEqualTo("해당 워크스페이스에 존재하지 않는 수신자입니다. ID: " + recipientId);
    }

    @Test
    @DisplayName("중복 수신자 검증 성공 테스트 - 중복이 없는 경우")
    void validateNoDuplicateRecipientExists_Success_NoDuplicate_Test() {
        // given
        // 1. 테스트용 워크스페이스를 생성합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        String recipientName = "김철수";
        String recipientPhoneNumber = "010-2222-2222";

        // 2. Mock Repository의 동작을 정의합니다: 중복이 없으므로 false를 반환하도록 설정합니다.
        when(recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber(
                mockWorkspace, recipientName, recipientPhoneNumber))
                .thenReturn(false);

        // when & then
        // 1. 중복이 없을 경우 예외가 발생하지 않아야 합니다.
        // 따라서 assertDoesNotThrow를 사용하여 예외가 발생하지 않음을 검증합니다.
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                recipientValidator.validateNoDuplicateRecipientExists(mockWorkspace, recipientName, recipientPhoneNumber));
    }

    @Test
    @DisplayName("중복 수신자 검증 실패 테스트 - 중복이 존재하는 경우")
    void validateNoDuplicateRecipientExists_Fail_DuplicateExists_Test() {
        // given
        // 1. 테스트용 워크스페이스를 생성합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        String recipientName = "이영희";
        String recipientPhoneNumber = "010-3333-3333";

        // 2. Mock Repository의 동작을 정의합니다: 중복이 존재하므로 true를 반환하도록 설정합니다.
        when(recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber(
                mockWorkspace, recipientName, recipientPhoneNumber))
                .thenReturn(true);

        // when & then
        // 1. validator 메서드 호출 시 IllegalArgumentException이 발생하는 것을 검증합니다.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> recipientValidator.validateNoDuplicateRecipientExists(mockWorkspace, recipientName, recipientPhoneNumber));

        // 2. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertThat(exception.getMessage()).isEqualTo("해당 워크스페이스에 동일한 이름과 번호의 수신자가 이미 존재합니다.");
    }

    @Test
    @DisplayName("수정 시 중복 수신자 검증 성공 테스트 - 중복이 없는 경우")
    void validateNoDuplicateRecipientExistsOnUpdate_Success_NoDuplicate_Test() {
        // given
        // 1. 테스트용 워크스페이스를 생성합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        String recipientName = "김철수";
        String recipientPhoneNumber = "010-2222-2222";
        Integer recipientId = 1;

        // 2. Mock Repository의 동작을 정의합니다: 자기 자신을 제외하고 중복이 없으므로 false를 반환하도록 설정합니다.
        when(recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot(
                mockWorkspace, recipientName, recipientPhoneNumber, recipientId))
                .thenReturn(false);

        // when & then
        // 1. 중복이 없을 경우 예외가 발생하지 않아야 합니다.
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                recipientValidator.validateNoDuplicateRecipientExistsOnUpdate(mockWorkspace, recipientName, recipientPhoneNumber, recipientId));
    }

    @Test
    @DisplayName("수정 시 중복 수신자 검증 실패 테스트 - 다른 수신자와 중복")
    void validateNoDuplicateRecipientExistsOnUpdate_Fail_DuplicateWithOther_Test() {
        // given
        // 1. 테스트용 워크스페이스를 생성합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        String recipientName = "이영희";
        String recipientPhoneNumber = "010-3333-3333";
        Integer recipientId = 1;

        // 2. Mock Repository의 동작을 정의합니다: 자기 자신을 제외하고 중복이 존재하므로 true를 반환하도록 설정합니다.
        when(recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot(
                mockWorkspace, recipientName, recipientPhoneNumber, recipientId))
                .thenReturn(true);

        // when & then
        // 1. validator 메서드 호출 시 IllegalArgumentException이 발생하는 것을 검증합니다.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> recipientValidator.validateNoDuplicateRecipientExistsOnUpdate(mockWorkspace, recipientName, recipientPhoneNumber, recipientId));

        // 2. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertThat(exception.getMessage()).isEqualTo("해당 정보와 동일한 다른 수신자가 이미 존재합니다.");
    }

    @Test
    @DisplayName("수신자 일괄 검증 및 조회 성공 테스트")
    void validateAndGetRecipients_Success_Test() {
        // given
        // 1. 테스트용 워크스페이스 ID와 검증할 수신자 ID 목록을 정의합니다.
        Integer workspaceId = 1;
        List<Integer> recipientIds = List.of(1, 2);
        // 2. Mock Repository가 반환할 수신자 엔티티 목록을 생성합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        List<Recipient> mockRecipients = List.of(
                Recipient.builder().recipientId(1).recipientName("홍길동").recipientPhoneNumber("010-1111-1111").workspace(mockWorkspace).build(),
                Recipient.builder().recipientId(2).recipientName("임꺽정").recipientPhoneNumber("010-1111-2222").workspace(mockWorkspace).build()
        );

        // 3. Mock Repository의 동작을 정의합니다: ID 목록으로 조회 시 mockRecipients를 반환하도록 설정합니다.
        when(recipientRepository.findAllByWorkspace_WorkspaceIdAndRecipientIdIn(workspaceId, recipientIds))
                .thenReturn(mockRecipients);

        // when
        // 1. 테스트 대상인 validator 메서드를 호출합니다.
        List<Recipient> result = recipientValidator.validateAndGetRecipients(workspaceId, recipientIds);

        // then
        // 1. 반환된 결과가 null이 아닌지 확인합니다.
        assertThat(result).isNotNull();
        // 2. 반환된 목록의 크기가 요청한 ID 목록의 크기와 같은지 확인합니다.
        assertThat(result.size()).isEqualTo(2);
        // 3. 반환된 수신자들의 ID가 예상과 일치하는지 확인합니다.
        assertThat(result).extracting(Recipient::getRecipientId).containsExactly(1, 2);
    }

    @Test
    @DisplayName("수신자 일괄 검증 및 조회 실패 테스트 - ID 개수 불일치")
    void validateAndGetRecipients_Fail_IdMismatch_Test() {
        // given
        // 1. 테스트용 워크스페이스 ID와 검증할 수신자 ID 목록을 정의합니다. (요청은 3개)
        Integer workspaceId = 1;
        List<Integer> recipientIds = List.of(1, 2, 3);
        // 2. Mock Repository가 반환할 목록을 생성합니다. (실제 DB에서는 2개만 조회된 상황)
        Workspace mockWorkspace = mock(Workspace.class);
        List<Recipient> mockRecipients = List.of(
                Recipient.builder().recipientId(1).recipientName("홍길동").recipientPhoneNumber("010-1111-1111").workspace(mockWorkspace).build(),
                Recipient.builder().recipientId(2).recipientName("임꺽정").recipientPhoneNumber("010-1111-2222").workspace(mockWorkspace).build()
        );

        // 3. Mock Repository의 동작을 정의합니다.
        when(recipientRepository.findAllByWorkspace_WorkspaceIdAndRecipientIdIn(workspaceId, recipientIds))
                .thenReturn(mockRecipients);

        // when & then
        // 1. validator 메서드 호출 시 IllegalArgumentException이 발생하는 것을 검증합니다.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> recipientValidator.validateAndGetRecipients(workspaceId, recipientIds));

        // 2. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertThat(exception.getMessage()).isEqualTo("요청된 수신자 목록에 유효하지 않거나 권한이 없는 ID가 포함되어 있습니다.");
    }
}
