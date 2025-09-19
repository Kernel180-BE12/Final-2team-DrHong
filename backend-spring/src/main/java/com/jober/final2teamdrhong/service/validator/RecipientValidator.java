package com.jober.final2teamdrhong.service.validator;

import com.jober.final2teamdrhong.entity.Recipient;
import com.jober.final2teamdrhong.repository.RecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecipientValidator {

    private final RecipientRepository recipientRepository;

    /**
     * 특정 워크스페이스에 해당 수신자가 존재하는지 검증합니다.
     * 검증에 성공하면 수신자 엔티티를 반환하고, 실패하면 예외를 발생시킵니다.
     *
     * @param workspaceId 수신자가 속한 워크스페이스의 ID
     * @param recipientId 검증할 수신자의 ID
     * @return 검증에 성공한 Recipient 엔티티
     * @throws IllegalArgumentException 해당 워크스페이스에 수신자가 존재하지 않을 경우
     */
    public Recipient validateAndGetRecipient(Integer workspaceId, Integer recipientId) {
        return recipientRepository.findByRecipientIdAndWorkspace_WorkspaceId(recipientId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스에 존재하지 않는 수신자입니다. ID: " + recipientId));
    }
}
