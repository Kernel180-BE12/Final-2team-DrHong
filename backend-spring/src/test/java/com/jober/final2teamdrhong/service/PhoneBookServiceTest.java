package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.phonebook.PhoneBookRequest;
import com.jober.final2teamdrhong.dto.phonebook.PhoneBookResponse;
import com.jober.final2teamdrhong.entity.GroupMapping;
import com.jober.final2teamdrhong.entity.PhoneBook;
import com.jober.final2teamdrhong.entity.Recipient;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.GroupMappingRepository;
import com.jober.final2teamdrhong.repository.PhoneBookRepository;
import com.jober.final2teamdrhong.service.validator.PhoneBookValidator;
import com.jober.final2teamdrhong.service.validator.RecipientValidator;
import com.jober.final2teamdrhong.service.validator.WorkspaceValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhoneBookServiceTest {

    @Mock
    private PhoneBookRepository phoneBookRepository;

    @Mock
    private GroupMappingRepository groupMappingRepository;

    @Mock
    private WorkspaceValidator workspaceValidator;

    @Mock
    private PhoneBookValidator phoneBookValidator;

    @Mock
    private RecipientValidator recipientValidator;

    @InjectMocks
    private PhoneBookService phoneBookService;

    @Test
    @DisplayName("주소록 생성 성공 테스트")
    void createPhoneBook_Success_Test() {
        // given
        // 1. 테스트에 사용할 ID와 요청 DTO를 준비합니다.
        Integer userId = 1;
        Integer workspaceId = 1;
        PhoneBookRequest.CreateDTO createDTO = PhoneBookRequest.CreateDTO.builder()
                .phoneBookName("테스트 주소록")
                .phoneBookMemo("테스트 메모입니다.")
                .build();

        // 2. Mock 객체들을 준비합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        PhoneBook phoneBookToSave = PhoneBook.builder()
                .phoneBookId(1) // DTO 변환을 위해 ID 설정
                .phoneBookName(createDTO.getPhoneBookName())
                .phoneBookMemo(createDTO.getPhoneBookMemo())
                .workspace(mockWorkspace)
                .build();

        // 3. Mockito 행동 정의
        //    - workspaceValidator.validateAndGetWorkspace 호출 시, mockWorkspace를 반환하여 권한 검증을 통과시킵니다.
        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId))
                .thenReturn(mockWorkspace);
        //    - phoneBookRepository.save 호출 시, 저장된 것처럼 phoneBookToSave 객체를 반환합니다.
        when(phoneBookRepository.save(any(PhoneBook.class)))
                .thenReturn(phoneBookToSave);

        // when
        // 1. 실제 테스트 대상인 서비스 메소드를 호출합니다.
        PhoneBookResponse.SimpleDTO result = phoneBookService.createPhoneBook(createDTO, workspaceId, userId);

        // then
        // 1. 반환된 DTO가 null이 아닌지, 그리고 필드 값들이 요청한 데이터와 일치하는지 검증합니다.
        assertNotNull(result);
        assertEquals(1, result.getPhoneBookId());
        assertEquals("테스트 주소록", result.getPhoneBookName());
        assertEquals("테스트 메모입니다.", result.getPhoneBookMemo());

        // 2. Validator와 Repository의 메소드가 각각 정확히 1번씩 호출되었는지 검증합니다.
        verify(workspaceValidator, times(1)).validateAndGetWorkspace(workspaceId, userId);
        verify(phoneBookRepository, times(1)).save(any(PhoneBook.class));
    }

    @Test
    @DisplayName("주소록 생성 실패 테스트 - 존재하지 않거나 권한이 없는 워크스페이스")
    void createPhoneBook_Fail_UnauthorizedWorkspace_Test() {
        // given
        // 1. 테스트에 사용할 ID와 요청 DTO를 준비합니다.
        Integer userId = 1;
        Integer unauthorizedWorkspaceId = 999;
        PhoneBookRequest.CreateDTO createDTO = new PhoneBookRequest.CreateDTO();

        // 2. Mockito 행동 정의: workspaceValidator가 예외를 던지도록 설정합니다.
        when(workspaceValidator.validateAndGetWorkspace(unauthorizedWorkspaceId, userId))
                .thenThrow(new IllegalArgumentException("워크스페이스를 찾을 수 없거나 접근권한이 없습니다. ID: " + unauthorizedWorkspaceId));

        // when
        // 1. 서비스 메소드를 호출했을 때 특정 예외가 발생하는지 검증합니다.
        Throwable thrown = assertThrows(IllegalArgumentException.class, () ->
                phoneBookService.createPhoneBook(createDTO, unauthorizedWorkspaceId, userId));

        // then
        // 1. 발생한 예외의 메시지가 예상과 정확히 일치하는지 확인합니다.
        assertEquals("워크스페이스를 찾을 수 없거나 접근권한이 없습니다. ID: " + unauthorizedWorkspaceId,
                thrown.getMessage());

        // 2. (중요) 로직이 초반에 중단되었으므로, 주소록을 저장하는 로직은 절대 호출되면 안됩니다.
        verify(phoneBookRepository, never()).save(any(PhoneBook.class));
    }

    @Test
    @DisplayName("주소록에 수신자 일괄 추가 성공 테스트 - 신규 수신자만 추가")
    void addRecipientsToPhoneBook_Success_AddNewRecipients_Test() {
        // given
        // 1. 테스트용 ID와 요청 DTO를 정의합니다. (수신자 ID 2, 3 추가 요청)
        Integer workspaceId = 1, phoneBookId = 1, userId = 1;
        PhoneBookRequest.RecipientIdListDTO requestDTO = new PhoneBookRequest.RecipientIdListDTO(List.of(2, 3));

        // 2. Mock 객체들을 준비합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        PhoneBook mockPhoneBook = PhoneBook.builder()
                .phoneBookId(phoneBookId)
                .phoneBookName("테스트 주소록")
                .workspace(mockWorkspace)
                .build();
        List<Recipient> mockRecipients = List.of(
                Recipient.builder()
                        .recipientId(2)
                        .recipientName("홍길동")
                        .recipientPhoneNumber("010-1111-1111")
                        .workspace(mockWorkspace)
                        .build(),
                Recipient.builder()
                        .recipientId(3)
                        .recipientName("임꺽정")
                        .recipientPhoneNumber("010-1111-2222")
                        .workspace(mockWorkspace)
                        .build()
        );
        List<Integer> existingRecipientIds = List.of(1); // 기존에는 1번 수신자만 존재한다고 가정

        // 3. Mock Validator들의 동작을 정의합니다.
        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId))
                .thenReturn(mockWorkspace); // 워크스페이스 검증 통과
        when(phoneBookValidator.validateAndGetPhoneBook(workspaceId, phoneBookId))
                .thenReturn(mockPhoneBook); // 주소록 검증 통과
        when(recipientValidator.validateAndGetRecipients(workspaceId, requestDTO.getRecipientIds()))
                .thenReturn(mockRecipients); // 수신자 검증 통과

        // 4. Mock Repository들의 동작을 정의합니다.
        when(groupMappingRepository.findRecipientIdsByPhoneBook(mockPhoneBook))
                .thenReturn(existingRecipientIds); // 기존 수신자 ID 목록 반환

        // 벌크 INSERT 후 생성된 매핑들을 모방합니다.
        List<GroupMapping> savedMappings = List.of(
                GroupMapping.builder()
                        .phoneBook(mockPhoneBook)
                        .recipient(mockRecipients.get(0))
                        .build(),
                GroupMapping.builder()
                        .phoneBook(mockPhoneBook)
                        .recipient(mockRecipients.get(1))
                        .build()
        );
        when(groupMappingRepository.findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn(
                eq(phoneBookId), eq(List.of(2, 3))))
                .thenReturn(savedMappings);

        // when
        // 1. 테스트 대상 서비스 메서드를 호출합니다.
        PhoneBookResponse.ModifiedRecipientsDTO result = phoneBookService.addRecipientsToPhoneBook(requestDTO, workspaceId, phoneBookId, userId);

        // then
        // 1. 결과 DTO가 null이 아닌지 확인합니다.
        assertThat(result).isNotNull();
        // 2. 결과 DTO의 주소록 ID가 올바른지 확인합니다.
        assertThat(result.getPhoneBookId()).isEqualTo(phoneBookId);
        // 3. 실제로 추가된 수신자 목록의 크기가 2인지 확인합니다.
        assertThat(result.getRecipientList().size()).isEqualTo(2);
        // 4. 추가된 수신자들의 ID가 2와 3인지 확인합니다.
        assertThat(result.getRecipientList()).extracting("recipientId").containsExactlyInAnyOrder(2, 3);
        // 5. bulkInsertMappings와 findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn가 정확히 1번씩 호출되었는지 검증합니다.
        verify(groupMappingRepository, times(1)).bulkInsertMappings(eq(phoneBookId), eq(List.of(2, 3)), any(LocalDateTime.class));
        verify(groupMappingRepository, times(1)).findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn(eq(phoneBookId), eq(List.of(2, 3)));
    }

    @Test
    @DisplayName("주소록에 수신자 일괄 추가 테스트 - 일부는 중복, 일부는 신규")
    void addRecipientsToPhoneBook_Success_MixDuplicateAndNew_Test() {
        // given
        // 1. 테스트용 ID와 요청 DTO를 정의합니다. (1번은 중복, 2번은 신규)
        Integer workspaceId = 1, phoneBookId = 1, userId = 1;
        PhoneBookRequest.RecipientIdListDTO requestDTO = new PhoneBookRequest.RecipientIdListDTO(List.of(1, 2));

        // 2. Mock 객체들을 준비합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        PhoneBook mockPhoneBook = PhoneBook.builder()
                .phoneBookId(phoneBookId)
                .phoneBookName("테스트 주소록")
                .workspace(mockWorkspace)
                .build();
        List<Recipient> mockRecipients = List.of(
                Recipient.builder()
                        .recipientId(1)
                        .recipientName("홍길동")
                        .recipientPhoneNumber("010-1111-1111")
                        .workspace(mockWorkspace)
                        .build(),
                Recipient.builder()
                        .recipientId(2)
                        .recipientName("임꺽정")
                        .recipientPhoneNumber("010-1111-2222")
                        .workspace(mockWorkspace)
                        .build()
        );
        List<Integer> existingRecipientIds = List.of(1); // 기존에 1번 수신자가 이미 존재

        // 3. Mock 객체들의 동작을 정의합니다.
        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId))
                .thenReturn(mockWorkspace);
        when(phoneBookValidator.validateAndGetPhoneBook(workspaceId, phoneBookId))
                .thenReturn(mockPhoneBook);
        when(recipientValidator.validateAndGetRecipients(workspaceId, requestDTO.getRecipientIds()))
                .thenReturn(mockRecipients);
        when(groupMappingRepository.findRecipientIdsByPhoneBook(mockPhoneBook))
                .thenReturn(existingRecipientIds);

        // 새로 추가될 수신자(ID=2)에 대한 매핑을 모방합니다.
        List<GroupMapping> savedMappings = List.of(
                GroupMapping.builder()
                        .phoneBook(mockPhoneBook)
                        .recipient(mockRecipients.get(1)) // ID=2인 수신자
                        .build()
        );
        when(groupMappingRepository.findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn(
                eq(phoneBookId), eq(List.of(2))))
                .thenReturn(savedMappings);

        // when
        // 1. 테스트 대상 서비스 메서드를 호출합니다.
        PhoneBookResponse.ModifiedRecipientsDTO result = phoneBookService.addRecipientsToPhoneBook(requestDTO, workspaceId, phoneBookId, userId);

        // then
        // 1. 결과 DTO가 null이 아닌지 확인합니다.
        assertThat(result).isNotNull();
        // 2. 실제로 추가된 수신자는 1명(2번)이어야 합니다.
        assertThat(result.getRecipientList().size()).isEqualTo(1);
        // 3. 추가된 수신자의 ID가 2번인지 확인합니다.
        assertThat(result.getRecipientList().getFirst().getRecipientId()).isEqualTo(2);
        // 4. bulkInsertMappings와 findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn가 정확히 1번씩 호출되었는지 검증합니다.
        verify(groupMappingRepository, times(1)).bulkInsertMappings(eq(phoneBookId), eq(List.of(2)), any(LocalDateTime.class));
        verify(groupMappingRepository, times(1)).findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn(eq(phoneBookId), eq(List.of(2)));
    }

    @Test
    @DisplayName("주소록에 수신자 일괄 추가 테스트 - 추가할 신규 수신자가 없는 경우")
    void addRecipientsToPhoneBook_NoNewRecipientsToAdd_Test() {
        // given
        // 1. 테스트용 ID와 요청 DTO를 정의합니다. (요청한 1번 수신자가 이미 존재)
        Integer workspaceId = 1, phoneBookId = 1, userId = 1;
        PhoneBookRequest.RecipientIdListDTO requestDTO = new PhoneBookRequest.RecipientIdListDTO(List.of(1));

        // 2. Mock 객체들을 준비합니다.
        Workspace mockWorkspace = mock(Workspace.class);
        PhoneBook mockPhoneBook = PhoneBook.builder()
                .phoneBookId(phoneBookId)
                .phoneBookName("테스트 주소록")
                .workspace(mockWorkspace)
                .build();
        List<Recipient> mockRecipients = List.of(Recipient.builder()
                .recipientId(1)
                .recipientName("임꺽정")
                .recipientPhoneNumber("010-1111-2222")
                .workspace(mockWorkspace)
                .build());
        List<Integer> existingRecipientIds = List.of(1);

        // 3. Mock 객체들의 동작을 정의합니다.
        when(workspaceValidator.validateAndGetWorkspace(workspaceId, userId))
                .thenReturn(mockWorkspace);
        when(phoneBookValidator.validateAndGetPhoneBook(workspaceId, phoneBookId))
                .thenReturn(mockPhoneBook);
        when(recipientValidator.validateAndGetRecipients(workspaceId, requestDTO.getRecipientIds()))
                .thenReturn(mockRecipients);
        when(groupMappingRepository.findRecipientIdsByPhoneBook(mockPhoneBook))
                .thenReturn(existingRecipientIds);

        // when
        // 1. 테스트 대상 서비스 메서드를 호출합니다.
        PhoneBookResponse.ModifiedRecipientsDTO result = phoneBookService.addRecipientsToPhoneBook(requestDTO, workspaceId, phoneBookId, userId);

        // then
        // 1. 결과 DTO가 null이 아닌지 확인합니다.
        assertThat(result).isNotNull();
        // 2. 추가된 수신자 목록이 비어있는지 확인합니다.
        assertThat(result.getRecipientList()).isEmpty();
        // 3. (중요) 신규 추가할 수신자가 없으므로 bulkInsertMappings와 findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn 메서드가 호출되지 않아야 합니다.
        verify(groupMappingRepository, never()).bulkInsertMappings(any(), any(), any());
        verify(groupMappingRepository, never()).findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn(any(), any());
    }
}
