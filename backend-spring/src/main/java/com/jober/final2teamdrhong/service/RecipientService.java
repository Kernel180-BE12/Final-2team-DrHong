package com.jober.final2teamdrhong.service;

import com.jober.final2teamdrhong.dto.recipient.RecipientRequest;
import com.jober.final2teamdrhong.dto.recipient.RecipientResponse;
import com.jober.final2teamdrhong.entity.Recipient;
import com.jober.final2teamdrhong.entity.Workspace;
import com.jober.final2teamdrhong.repository.RecipientRepository;
import com.jober.final2teamdrhong.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 수신자(Recipient) 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipientService {

    private final RecipientRepository recipientRepository;
    private final WorkspaceRepository workspaceRepository;

    /**
     * 특정 워크스페이스에 새로운 수신자를 생성합니다.
     *
     * @param createDTO   수신자 생성을 위한 요청 데이터
     * @param workspaceId 수신자를 추가할 워크스페이스의 ID
     * @param userId      요청을 보낸 사용자의 ID (인가에 사용)
     * @return 생성된 수신자의 정보({@link RecipientResponse.SimpleDTO})
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 접근 권한이 없을 경우 발생
     */
    @Transactional
    public RecipientResponse.SimpleDTO createRecipient(RecipientRequest.CreateDTO createDTO, Integer workspaceId, Integer userId) {
        // 1. 인가(Authorization): 요청한 사용자가 워크스페이스에 접근 권한이 있는지 확인합니다.
        Workspace workspace = workspaceRepository.findByWorkspaceIdAndUser_UserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없거나 접근권한이 없습니다. ID: " + workspaceId));

        // 2. 엔티티 생성: DTO의 데이터를 기반으로 Recipient 엔티티를 생성합니다.
        Recipient recipient = Recipient.builder()
                .recipientName(createDTO.getRecipientName())
                .recipientPhoneNumber(createDTO.getRecipientPhoneNumber())
                .recipientMemo(createDTO.getRecipientMemo())
                .workspace(workspace)
                .build();

        // 3. 엔티티 저장 및 DTO 변환 후 반환
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
        workspaceRepository.findByWorkspaceIdAndUser_UserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없거나 접근권한이 없습니다. ID: " + workspaceId));

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
     * @throws IllegalArgumentException 워크스페이스나 수신자를 찾을 수 없거나, 사용자가 접근 권한이 없을 경우 발생
     */
    @Transactional
    public RecipientResponse.SimpleDTO updateRecipient(RecipientRequest.UpdateDTO updateDTO,
                                                       Integer workspaceId, Integer recipientId, Integer userId) {
        // 1. 워크스페이스 접근 권한 확인
        workspaceRepository.findByWorkspaceIdAndUser_UserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없거나 접근권한이 없습니다. ID: " + workspaceId));

        // 2. 수신자 조회 (워크스페이스 소속인지 함께 검증)
        Recipient existingRecipient = recipientRepository.findByRecipientIdAndWorkspace_WorkspaceId(recipientId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스에 존재하지 않는 수신자입니다. ID: " + recipientId));

        // 3. 정보 업데이트
        existingRecipient.setRecipientName(updateDTO.getNewRecipientName());
        existingRecipient.setRecipientPhoneNumber(updateDTO.getNewRecipientPhoneNumber());
        existingRecipient.setRecipientMemo(updateDTO.getNewRecipientMemo());
        existingRecipient.setUpdatedAt(LocalDateTime.now());

        return new RecipientResponse.SimpleDTO(existingRecipient);
    }

    /**
     * 특정 수신자를 삭제합니다 (소프트 딜리트).
     * <p>
     * 요청한 사용자가 해당 워크스페이스의 소유자인지 확인하는 인가 과정이 포함되며,
     * 실제 데이터베이스에서 삭제되지 않고 is_deleted 플래그를 true로 변경하고 deleted_at에 현재 시간을 설정합니다.
     * <p>
     * JPA의 Dirty Checking 기능을 통해 변경사항이 자동으로 데이터베이스에 UPDATE됩니다.
     *
     * @param workspaceId 삭제할 수신자가 속한 워크스페이스의 ID
     * @param recipientId 삭제할 수신자의 ID
     * @param userId      삭제를 요청한 사용자의 ID (인가에 사용)
     * @return 삭제 처리된 수신자의 정보가 담긴 {@link RecipientResponse.SimpleDTO}
     * @throws IllegalArgumentException 해당 워크스페이스가 존재하지 않거나, 사용자가 접근 권한이 없거나,
     *                                  수신자가 해당 워크스페이스에 존재하지 않을 경우 발생
     */
    @Transactional
    public RecipientResponse.SimpleDTO deleteRecipient(Integer workspaceId, Integer recipientId, Integer userId) {
        // 1. 워크스페이스 접근 권한 확인
        workspaceRepository.findByWorkspaceIdAndUser_UserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("워크스페이스를 찾을 수 없거나 접근권한이 없습니다. ID: " + workspaceId));

        // 2. 수신자 조회 (워크스페이스 소속인지 함께 검증)
        Recipient existingRecipient = recipientRepository.findByRecipientIdAndWorkspace_WorkspaceId(recipientId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스에 존재하지 않는 수신자입니다. ID: " + recipientId));

        // 3. 소프트 딜리트 처리
        existingRecipient.setDeleted(true);
        existingRecipient.setUpdatedAt(LocalDateTime.now());
        existingRecipient.setDeletedAt(LocalDateTime.now());

        return new RecipientResponse.SimpleDTO(existingRecipient);
    }
}
