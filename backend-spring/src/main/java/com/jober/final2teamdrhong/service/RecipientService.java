package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.recipient.RecipientRequest;
import com.jober.final2teamdrhong.dto.recipient.RecipientResponse;
import com.jober.final2teamdrhong.entity.Recipient;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.RecipientRepository;
import com.jober.final2teamdrhong.service.validator.RecipientValidator;
import com.jober.final2teamdrhong.service.validator.WorkspaceValidator;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 수신자(Recipient) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final RecipientValidator recipientValidator;
    private final WorkspaceValidator workspaceValidator;
    private final EntityManager entityManager;

    /**
     * 특정 워크스페이스에 새로운 수신자를 생성합니다.
     * <p>
     * 수신자를 생성하기 전에, 해당 워크스페이스에 동일한 이름과 전화번호를 가진 수신자가 이미 존재하는지 확인합니다.
     *
     * @param createDTO   수신자 생성을 위한 요청 데이터
     * @param workspaceId 수신자를 추가할 워크스페이스의 ID
     * @param userId      요청을 보낸 사용자의 ID (인가에 사용)
     * @return 생성된 수신자의 정보({@link RecipientResponse.SimpleDTO})
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 접근 권한이 없을 경우,
     *                                  또는 동일한 이름과 번호의 수신자가 이미 존재할 경우 발생
     */
    @Transactional
    public RecipientResponse.SimpleDTO createRecipient(RecipientRequest.CreateDTO createDTO, Integer workspaceId, Integer userId) {
        // 1. 인가(Authorization): 요청한 사용자가 워크스페이스에 접근 권한이 있는지 확인합니다.
        Workspace workspace = workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        // 2. 중복 확인: 해당 워크스페이스에 동일한 이름과 번호의 수신자가 이미 존재하는지 검증합니다.
        recipientValidator.validateNoDuplicateRecipientExists(workspace, createDTO.getRecipientName(), createDTO.getRecipientPhoneNumber());

        // 3. 엔티티 생성: DTO의 데이터를 기반으로 Recipient 엔티티를 생성합니다.
        Recipient recipient = Recipient.builder()
                .recipientName(createDTO.getRecipientName())
                .recipientPhoneNumber(createDTO.getRecipientPhoneNumber())
                .recipientMemo(createDTO.getRecipientMemo())
                .workspace(workspace)
                .build();

        // 4. 엔티티 저장 및 DTO 변환 후 반환
        Recipient savedRecipient = recipientRepository.save(recipient);

        return new RecipientResponse.SimpleDTO(savedRecipient);
    }

    /**
     * 특정 워크스페이스에 속한 모든 수신자 목록을 페이징하여 조회합니다.
     * <p>
     * 이 메서드는 먼저 요청한 사용자가 해당 워크스페이스에 대한 접근 권한이 있는지 확인합니다.
     * 권한이 확인되면, 해당 워크스페이스의 모든 수신자 정보를 DTO 리스트로 변환하여 반환합니다.
     *
     * @param workspaceId 수신자 목록을 조회할 워크스페이스의 ID
     * @param userId      요청을 보낸 사용자의 ID (인가에 사용)
     * @param pageable 페이징 및 정렬 요청 정보
     * @return 페이징 처리된 수신자 정보가 담긴 Page<{@link RecipientResponse.SimpleDTO}> 객체
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 접근 권한이 없을 경우 발생
     */
    public Page<RecipientResponse.SimpleDTO> readRecipients(Integer workspaceId, Integer userId, Pageable pageable) {
        workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        Page<Recipient> recipientPage = recipientRepository.findAllByWorkspace_WorkspaceId(workspaceId, pageable);

        return recipientPage.map(RecipientResponse.SimpleDTO::new);
    }

    /**
     * 특정 수신자의 정보를 수정합니다.
     * <p>
     * 이 메소드는 다음의 순서로 동작합니다:
     * <ol>
     *     <li>요청한 사용자가 대상 워크스페이스에 대한 접근 권한이 있는지 확인합니다.</li>
     *     <li>수정하려는 수신자가 해당 워크스페이스에 실제로 속해 있는지 검증합니다.</li>
     *     <li>변경하려는 이름과 전화번호가 (자기 자신을 제외한) 다른 수신자와 중복되지 않는지 검증합니다.</li>
     *     <li>검증이 완료되면, DTO로부터 받은 새로운 정보로 수신자 엔티티의 상태를 변경합니다.</li>
     * </ol>
     * 메소드에 {@link Transactional} 어노테이션이 적용되어 있어,
     * 메소드 종료 시 변경된 엔티티 정보(Dirty Checking)가 데이터베이스에 자동으로 반영됩니다.
     *
     * @param updateDTO   수신자 수정을 위한 새로운 데이터
     * @param workspaceId 수정할 수신자가 속한 워크스페이스의 ID
     * @param recipientId 수정할 수신자의 ID
     * @param userId      요청을 보낸 사용자의 ID (인가에 사용)
     * @return 수정된 수신자의 정보가 담긴 {@link RecipientResponse.SimpleDTO}
     * @throws IllegalArgumentException 워크스페이스나 수신자를 찾을 수 없거나, 사용자가 접근 권한이 없거나,
     *                                  변경하려는 정보가 다른 수신자와 중복될 경우 발생
     */
    @Transactional
    public RecipientResponse.SimpleDTO updateRecipient(RecipientRequest.UpdateDTO updateDTO,
                                                       Integer workspaceId, Integer recipientId, Integer userId) {
        // 1. 워크스페이스 접근 권한 확인
        workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        // 2. 수신자 조회 (워크스페이스 소속인지 함께 검증)
        Recipient existingRecipient = recipientValidator.validateAndGetRecipient(workspaceId, recipientId);

        // 3. 수정하려는 정보가 다른 수신자와 중복되는지 검증
        recipientValidator.validateNoDuplicateRecipientExistsOnUpdate(existingRecipient.getWorkspace(), updateDTO.getNewRecipientName(), updateDTO.getNewRecipientPhoneNumber(), recipientId);

        // 4. 정보 업데이트
        existingRecipient.setRecipientName(updateDTO.getNewRecipientName());
        existingRecipient.setRecipientPhoneNumber(updateDTO.getNewRecipientPhoneNumber());
        existingRecipient.setRecipientMemo(updateDTO.getNewRecipientMemo());
        existingRecipient.update();

        return new RecipientResponse.SimpleDTO(existingRecipient);
    }

    /**
     * 특정 수신자를 삭제합니다 (소프트 딜리트).
     * <p>
     * 요청한 사용자가 해당 워크스페이스의 소유자인지 확인하는 인가 과정이 포함되며,
     * 실제 데이터베이스에서 삭제되지 않고 is_deleted 플래그를 true로 변경하고 deleted_at에 현재 시간을 설정합니다.
     * <p>
     * 소프트 딜리트 처리 후, EntityManager의 flush()와 clear()를 통해 즉시 DB에 반영하고
     * 1차 캐시를 비운 다음, 네이티브 쿼리를 사용하여 삭제된 엔티티를 재조회합니다.
     * 이를 통해 정확한 삭제 시간(deletedAt)이 포함된 응답을 반환할 수 있습니다.
     *
     * @param workspaceId 삭제할 수신자가 속한 워크스페이스의 ID
     * @param recipientId 삭제할 수신자의 ID
     * @param userId      삭제를 요청한 사용자의 ID (인가에 사용)
     * @return 삭제 처리된 수신자의 정보가 담긴 {@link RecipientResponse.SimpleDTO}
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 접근 권한이 없거나,
     *                                  수신자가 해당 워크스페이스에 존재하지 않을 경우 발생
     * @throws IllegalStateException    소프트 딜리트 후 엔티티를 재조회하는 과정에서 문제가 발생했을 경우 발생
     */
    @Transactional
    public RecipientResponse.SimpleDTO deleteRecipient(Integer workspaceId, Integer recipientId, Integer userId) {
        // 1. 워크스페이스 접근 권한 확인
        workspaceValidator.validateAndGetWorkspace(workspaceId, userId);

        // 2. 수신자 조회 (워크스페이스 소속인지 함께 검증)
        Recipient existingRecipient = recipientValidator.validateAndGetRecipient(workspaceId, recipientId);

        // 3. 소프트 딜리트 처리
        existingRecipient.softDelete();

        // 4. 즉시 DB에 반영, 및 Hibernate 1차 캐시 비우기 후 변경된 DB를 반환해야 정확한 시간이 응답으로 나옴
        entityManager.flush();
        entityManager.clear();

        // 5. @SQLRestriction을 우회하는 네이티브 쿼리로 재조회하여 시간 동기화
        Recipient deletedRecipient = recipientRepository.findByIdIncludingDeleted(recipientId)
                .orElseThrow(() -> new IllegalStateException("소프트 딜리트 처리된 수신자를 재조회하는 데 실패했습니다. ID: " + recipientId));

        return new RecipientResponse.SimpleDTO(deletedRecipient);
    }
}
