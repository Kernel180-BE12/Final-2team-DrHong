package com.jober.final2teamdrhong.service.validator;

import com.jober.final2teamdrhong.entity.PhoneBook;
import com.jober.final2teamdrhong.repository.PhoneBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PhoneBookValidator {

    private final PhoneBookRepository phoneBookRepository;

    /**
     * 특정 워크스페이스에 해당 주소록이 존재하는지 검증합니다.
     * 검증에 성공하면 주소록 엔티티를 반환하고, 실패하면 예외를 발생시킵니다.
     *
     * @param workspaceId 주소록이 속한 워크스페이스의 ID
     * @param phoneBookId 검증할 주소록의 ID
     * @return 검증에 성공한 PhoneBook 엔티티
     * @throws IllegalArgumentException 해당 워크스페이스에 주소록이 존재하지 않을 경우
     */
    public PhoneBook validateAndGetPhoneBook(Integer workspaceId, Integer phoneBookId) {
        return phoneBookRepository.findByPhoneBookIdAndWorkspace_WorkspaceId(phoneBookId, workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스에 존재하지 않는 주소록입니다. ID: " + phoneBookId));
    }
}
