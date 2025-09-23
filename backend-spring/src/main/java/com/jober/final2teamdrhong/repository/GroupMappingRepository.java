package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.GroupMapping;
import com.jober.final2teamdrhong.entity.PhoneBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GroupMappingRepository extends JpaRepository<GroupMapping, Integer> {

    /**
     * 특정 주소록에 이미 매핑된 모든 수신자들의 ID 목록을 조회합니다.
     * <p>
     * 이 메소드는 주소록에 새로운 수신자를 추가하기 전,
     * 중복 추가를 방지하기 위한 목적으로 사용됩니다.
     *
     * @param phoneBook 조회할 주소록 엔티티
     * @return 해당 주소록에 속한 수신자들의 ID 리스트
     */
    @Query("""
            SELECT gm.recipient.recipientId 
            FROM GroupMapping gm 
            WHERE gm.phoneBook = :phoneBook""")
    List<Integer> findRecipientIdsByPhoneBook(@Param("phoneBook") PhoneBook phoneBook);

    /**
     * 여러 수신자를 특정 주소록에 일괄 추가합니다 (벌크 INSERT).
     * <p>
     * 네이티브 쿼리를 사용하여 단일 INSERT 쿼리로 여러 매핑을 한 번에 생성합니다.
     * 모든 매핑에 동일한 생성 시간이 적용되어 시간 일관성을 보장합니다.
     * <p>
     * 생성되는 필드들:
     * <ul>
     *     <li>phone_book_id: 파라미터로 전달받은 주소록 ID</li>
     *     <li>recipient_id: 파라미터로 전달받은 수신자 ID 목록</li>
     *     <li>created_at: 파라미터로 전달받은 현재 시간</li>
     *     <li>updated_at: 파라미터로 전달받은 현재 시간</li>
     *     <li>is_deleted: false (기본값)</li>
     *     <li>deleted_at: null (기본값)</li>
     * </ul>
     *
     * @param phoneBookId  매핑을 추가할 주소록의 ID
     * @param recipientIds 매핑을 추가할 수신자 ID 목록
     * @param now          생성 시간으로 사용할 현재 시간
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
                    INSERT INTO group_mapping (phone_book_id, recipient_id, created_at, updated_at, deleted_at, is_deleted)
                    SELECT :phoneBookId, r.recipient_id, :now, :now, null, false 
                    FROM recipient r
                    WHERE r.recipient_id IN :recipientIds""",
                    nativeQuery = true)
    void bulkInsertMappings(@Param("phoneBookId") Integer phoneBookId,
                            @Param("recipientIds") List<Integer> recipientIds,
                            @Param("now") LocalDateTime now);

    /**
     * 특정 주소록과 수신자 ID 목록에 해당하는 GroupMapping들을 조회합니다.
     * <p>
     * 이 메서드는 벌크 INSERT 후 생성된 매핑들을 재조회하는 용도로 사용됩니다.
     * {@code @SQLRestriction("is_deleted = false")} 제약 조건이 자동으로 적용되어
     * 삭제되지 않은 매핑만 조회됩니다.
     * <p>
     * 네이티브 쿼리 대신 JPA 쿼리 메서드를 사용하여 타입 안전성과 유지보수성을 향상시킵니다.
     * 벌크 INSERT로 생성된 매핑들은 동일한 생성 시간을 가지므로 별도 정렬은 불필요합니다.
     *
     * @param phoneBookId  조회할 주소록의 ID
     * @param recipientIds 조회할 수신자 ID 목록
     * @return 조건에 일치하는 GroupMapping 엔티티 목록
     */
    List<GroupMapping> findAllByPhoneBook_PhoneBookIdAndRecipient_RecipientIdIn(Integer phoneBookId, List<Integer> recipientIds);
}
