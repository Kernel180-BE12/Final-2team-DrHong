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
}
