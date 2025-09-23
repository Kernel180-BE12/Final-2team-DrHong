package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.PhoneBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhoneBookRepository extends JpaRepository<PhoneBook, Integer> {

    /**
     * 주소록 ID와 워크스페이스 ID를 사용하여 특정 워크스페이스에 속한 주소록을 조회합니다.
     * <p>
     * 이 메소드는 주소록에 수신자 추가, 삭제 등 특정 수신자에 대한 작업을 수행하기 전,
     * 해당 주소록이 올바른 워크스페이스에 속해 있는지 검증하는 데 사용될 수 있습니다.
     *
     * @param phoneBookId 조회할 주소록의 고유 ID
     * @param workspaceId 주소록이 속한 워크스페이스의 ID
     * @return 주소록 엔티티를 담은 Optional 객체. 해당하는 주소록이 없으면 Optional.empty()를 반환합니다.
     */
    Optional<PhoneBook> findByPhoneBookIdAndWorkspace_WorkspaceId(Integer phoneBookId, Integer workspaceId);
}
