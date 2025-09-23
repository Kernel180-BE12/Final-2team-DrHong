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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주소록(PhoneBook) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhoneBookService {

    private final PhoneBookRepository phoneBookRepository;
    private final GroupMappingRepository groupMappingRepository;
    private final PhoneBookValidator phoneBookValidator;
    private final WorkspaceValidator workspaceValidator;
    private final RecipientValidator recipientValidator;

    /**
     * 특정 워크스페이스에 새로운 주소록을 생성합니다.
     *
     * @param createDTO   주소록 생성을 위한 요청 데이터
     * @param workspaceId 주소록을 추가할 워크스페이스의 ID
     * @param userId      요청을 보낸 사용자의 ID (인가에 사용)
     * @return 생성된 주소록의 정보({@link PhoneBookResponse.SimpleDTO})
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 접근 권한이 없을 경우 발생
     */
    @Transactional
    public PhoneBookResponse.SimpleDTO createPhoneBook(PhoneBookRequest.CreateDTO createDTO, Integer workspaceId, Integer userId) {
        // 1. 인가(Authorization): 요청한 사용자가 워크스페이스에 접근 권한이 있는지 확인합니다.
        Workspace workspace = workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        // 2. 엔티티 생성: DTO의 데이터를 기반으로 PhoneBook 엔티티를 생성합니다.
        PhoneBook phoneBook = PhoneBook.builder()
                .phoneBookName(createDTO.getPhoneBookName())
                .phoneBookMemo(createDTO.getPhoneBookMemo())
                .workspace(workspace)
                .build();

        // 3. 엔티티 저장 및 DTO 변환 후 반환
        PhoneBook createdPhoneBook = phoneBookRepository.save(phoneBook);

        return new PhoneBookResponse.SimpleDTO(createdPhoneBook);
    }

    /**
     * 주소록에 다수의 수신자를 일괄 추가합니다.
     * <p>
     * 전체 로직은 하나의 트랜잭션으로 처리됩니다:
     * <ol>
     *     <li>요청된 워크스페이스, 주소록, 수신자 ID 목록의 유효성을 검증합니다.</li>
     *     <li>요청된 수신자 중 이미 주소록에 존재하는 멤버를 필터링하여 중복 추가를 방지합니다.</li>
     *     <li>실제로 추가할 신규 수신자가 없는 경우, 빈 목록을 포함한 DTO를 반환합니다.</li>
     *     <li>서울 시간대로 통일된 타임스탬프를 생성하고, 네이티브 쿼리를 사용하여
     *         신규 수신자들을 주소록에 일괄 추가합니다 (Bulk Insert).</li>
     *     <li>벌크 INSERT 후 실제 DB에 저장된 GroupMapping 엔티티들을 재조회하여
     *         {@link PhoneBookResponse.ModifiedRecipientsDTO#ofAddition(PhoneBook, List)}
     *         팩토리 메소드를 호출하여 결과 DTO를 생성하고 반환합니다.</li>
     * </ol>
     *
     * @param recipientIdListDTO 추가할 수신자 ID 목록을 담은 DTO
     * @param workspaceId      주소록이 속한 워크스페이스의 ID
     * @param phoneBookId      수신자를 추가할 주소록의 ID
     * @param userId           요청을 보낸 사용자의 ID (인가에 사용)
     * @return '추가' 이벤트의 결과로 생성된 {@link PhoneBookResponse.ModifiedRecipientsDTO}.
     *         실제로 추가된 수신자 목록과 DB에 기록된 작업 시간을 포함합니다.
     * @throws IllegalArgumentException 유효하지 않은 ID(워크스페이스, 주소록, 수신자)로 요청했을 경우 발생
     */
    @Transactional
    public PhoneBookResponse.ModifiedRecipientsDTO addRecipientsToPhoneBook(PhoneBookRequest.RecipientIdListDTO recipientIdListDTO, Integer workspaceId, Integer phoneBookId, Integer userId) {
        // 1. 폰북과 워크스페이스 권한 검증
        workspaceValidator.validateAndGetWorkspace(workspaceId, userId);
        PhoneBook phoneBook = phoneBookValidator.validateAndGetPhoneBook(workspaceId, phoneBookId);
        List<Recipient> allRequestedRecipients = recipientValidator.validateAndGetRecipients(workspaceId, recipientIdListDTO.getRecipientIds());

        // 2. 중복 방지를 위해 이미 존재하는 수신자 필터링
        List<Integer> existingRecipientIds = groupMappingRepository.findRecipientIdsByPhoneBook(phoneBook);

        // 2-1. 요청된 수신자 중, 실제로 새로 추가해야 할 수신자만 필터링합니다.
        List<Recipient> recipientsToActuallyAdd = allRequestedRecipients.stream()
                .filter(recipient -> !existingRecipientIds.contains(recipient.getRecipientId()))
                .toList();

        // 3. 새로 추가할 수신자가 없는 경우, 빈 목록으로 응답
        if (recipientsToActuallyAdd.isEmpty()) {
            // ofAddition 팩토리 메소드에 빈 GroupMapping 리스트를 전달하여
            // '추가된 멤버 없음'을 나타내는 DTO를 생성합니다.
            return PhoneBookResponse.ModifiedRecipientsDTO.ofAddition(phoneBook, new ArrayList<>());
        }

        // 4. 서울 시간대로 생성 시간을 설정
        LocalDateTime creationTimestamp = ZonedDateTime.now(ZoneId.of("Asia/Seoul")).toLocalDateTime();

        // 5. 추가할 수신자 ID 목록 추출
        List<Integer> recipientIdsToAdd = recipientsToActuallyAdd.stream()
                .map(Recipient::getRecipientId)
                .toList();

        // 6. 벌크 INSERT 실행 (단일 쿼리로 모든 매핑 생성, 동일한 시간 적용)
        groupMappingRepository.bulkInsertMappings(phoneBook.getPhoneBookId(), recipientIdsToAdd, creationTimestamp);

        // 7. 벌크 INSERT 후 실제 DB에서 생성된 매핑들을 재조회
        List<GroupMapping> savedMappings =
                groupMappingRepository.findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn(phoneBook.getPhoneBookId(), recipientIdsToAdd);

        // 8. '추가'용 팩토리 메소드를 호출하여 최종 DTO를 반환합니다.
        return PhoneBookResponse.ModifiedRecipientsDTO.ofAddition(phoneBook, savedMappings);
    }
}
