package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.Recipient;
import com.jober.final2teamdrhong.entity.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Integer> {

    /**
     * 특정 워크스페이스 내에서 동일한 이름과 전화번호를 가진 수신자가 존재하는지 확인합니다.
     *
     * @param workspace            검사를 수행할 워크스페이스 엔티티
     * @param recipientName        중복 여부를 확인할 수신자 이름
     * @param recipientPhoneNumber 중복 여부를 확인할 수신자 전화번호
     * @return 중복되는 수신자가 존재하면 {@code true}, 그렇지 않으면 {@code false}
     */
    boolean existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber(Workspace workspace, String recipientName, String recipientPhoneNumber);

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

    /**
     * 특정 수신자 ID를 제외하고, 워크스페이스 내에서 동일한 이름과 전화번호를 가진 수신자가 존재하는지 확인합니다.
     * (수신자 정보 수정 시 중복 검증을 위해 사용)
     *
     * @param workspace            검사를 수행할 워크스페이스 엔티티
     * @param recipientName        중복 여부를 확인할 수신자 이름
     * @param recipientPhoneNumber 중복 여부를 확인할 수신자 전화번호
     * @param recipientId          검사 대상에서 제외할 수신자의 ID
     * @return 중복되는 수신자가 존재하면 {@code true}, 그렇지 않으면 {@code false}
     */
    boolean existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot(Workspace workspace, String recipientName, String recipientPhoneNumber, Integer recipientId);

    /**
     * ID를 기준으로 수신자(Recipient) 엔티티를 조회합니다.
     * <p>
     * 이 메서드는 {@link Recipient} 엔티티에 적용된
     * {@code @SQLRestriction("is_deleted = false")} 제약 조건을 우회하기 위해 네이티브 SQL 쿼리를 사용합니다.
     * 따라서 소프트 딜리트 처리된 수신자를 포함하여 모든 상태의 수신자를 조회할 수 있습니다.
     * <p>
     * 주로 소프트 딜리트 트랜잭션 내에서 DB에 반영된 최종 상태(예: {@code deletedAt} 타임스탬프)를 정확히 다시 읽어와야 할 때 사용됩니다.
     *
     * @param recipientId 조회할 수신자의 ID
     * @return 조회된 Recipient 엔티티를 담은 Optional 객체. ID가 존재하지 않으면 빈 Optional을 반환합니다.
     */
    @Query(value = """
                    SELECT * 
                    FROM recipient 
                    WHERE recipient_id = :recipientId""",
                    nativeQuery = true)
    Optional<Recipient> findByIdIncludingDeleted(@Param("recipientId") Integer recipientId);

    /**
     * 특정 워크스페이스에 속하면서, 주어진 ID 목록에 포함되는 모든 수신자 엔티티를 조회합니다.
     * <p>
     * 이 메소드는 클라이언트로부터 받은 다수의 수신자 ID가 실제로 해당 워크스페이스에
     * 유효하게 존재하는지 한 번의 쿼리로 검증하는 데 사용됩니다.
     * 주소록에 여러 수신자를 추가하는 것과 같은 Bulk 작업 전에 효율적인 유효성 검사를 위해 사용될 수 있습니다.
     *
     * @param workspaceId  수신자들이 속한 워크스페이스의 ID
     * @param recipientIds 조회할 수신자들의 고유 ID 목록
     * @return 조회 조건에 일치하는 수신자 엔티티의 리스트. 일치하는 수신자가 없으면 빈 리스트를 반환합니다.
     */
    List<Recipient> findAllByWorkspace_WorkspaceIdAndRecipientIdIn(Integer workspaceId, List<Integer> recipientIds);
}
