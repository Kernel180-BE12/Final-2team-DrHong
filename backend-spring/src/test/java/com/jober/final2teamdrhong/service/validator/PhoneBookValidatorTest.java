package com.jober.final2teamdrhong.service.validator;

import com.jober.final2teamdrhong.entity.PhoneBook;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.PhoneBookRepository;
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
class PhoneBookValidatorTest {

    @Mock
    private PhoneBookRepository phoneBookRepository;

    @InjectMocks
    private PhoneBookValidator phoneBookValidator;

    @Test
    @DisplayName("주소록 검증 및 조회 성공 테스트")
    void validateAndGetPhoneBook_Success_Test() {
        // given
        // 1. 테스트에 사용할 워크스페이스 ID와 주소록 ID를 정의합니다.
        Integer workspaceId = 1;
        Integer phoneBookId = 1;
        // 2. Repository가 반환할 Mock 주소록 객체를 생성합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        PhoneBook mockPhoneBook = PhoneBook.builder()
                .phoneBookId(phoneBookId)
                .phoneBookName("test-phoneBook")
                .workspace(mockWorkspace)
                .build();

        // 3. Mock Repository의 동작을 정의합니다: 특정 ID로 조회 시 Mock 객체를 Optional에 담아 반환하도록 설정합니다.
        when(phoneBookRepository.findByPhoneBookIdAndWorkspace_WorkspaceId(phoneBookId, workspaceId))
                .thenReturn(Optional.of(mockPhoneBook));

        // when
        // 1. 테스트 대상인 validator 메서드를 호출합니다.
        PhoneBook result = phoneBookValidator.validateAndGetPhoneBook(workspaceId, phoneBookId);

        // then
        // 1. 반환된 결과가 null이 아닌지 확인합니다.
        assertThat(result).isNotNull();
        // 2. 반환된 주소록의 ID가 예상과 일치하는지 확인합니다.
        assertThat(result.getPhoneBookId()).isEqualTo(phoneBookId);
    }

    @Test
    @DisplayName("주소록 검증 및 조회 실패 테스트 - 존재하지 않는 주소록")
    void validateAndGetPhoneBook_Fail_NotFound_Test() {
        // given
        // 1. 테스트에 사용할 워크스페이스 ID와 주소록 ID를 정의합니다.
        Integer workspaceId = 1;
        Integer phoneBookId = 1;

        // 2. Mock Repository가 비어있는 Optional을 반환하도록 설정하여 '조회 실패' 상황을 시뮬레이션합니다.
        when(phoneBookRepository.findByPhoneBookIdAndWorkspace_WorkspaceId(phoneBookId, workspaceId))
                .thenReturn(Optional.empty());

        // when & then
        // 1. validator 메서드 호출 시 IllegalArgumentException이 발생하는 것을 검증합니다.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> phoneBookValidator.validateAndGetPhoneBook(workspaceId, phoneBookId));

        // 2. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertThat(exception.getMessage()).isEqualTo("해당 워크스페이스에 존재하지 않는 주소록입니다. ID: " + phoneBookId);
    }
}
