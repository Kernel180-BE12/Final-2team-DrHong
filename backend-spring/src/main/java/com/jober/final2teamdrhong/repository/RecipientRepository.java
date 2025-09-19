package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.Recipient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Integer> {

    /**
     * 특정 워크스페이스 ID에 해당하는 모든 수신자 목록을 조회합니다.
     *
     * @param workspaceId 수신자를 조회할 워크스페이스의 ID
     * @param pageable 클라이언트가 요청한 페이지 정보 (페이지 번호, 사이즈, 정렬)
     * @return 페이징 처리된 수신자 엔티티 정보 (데이터 목록 + 전체 페이지 수 등)
     */
    Page<Recipient> findAllByWorkspace_WorkspaceId(Integer workspaceId, Pageable pageable);

    /**
     * 수신자 ID와 워크스페이스 ID를 사용하여 특정 워크스페이스에 속한 수신자를 조회합니다.
     * <p>
     * 이 메소드는 수신자 정보 수정, 삭제 등 특정 수신자에 대한 작업을 수행하기 전,
     * 해당 수신자가 올바른 워크스페이스에 속해 있는지 검증하는 데 사용될 수 있습니다.
     *
     * @param recipientId 조회할 수신자의 고유 ID
     * @param workspaceId 수신자가 속한 워크스페이스의 ID
     * @return 수신자 엔티티를 담은 Optional 객체. 해당하는 수신자가 없으면 Optional.empty()를 반환합니다.
     */
    Optional<Recipient> findByRecipientIdAndWorkspace_WorkspaceId(Integer recipientId, Integer workspaceId);
}
