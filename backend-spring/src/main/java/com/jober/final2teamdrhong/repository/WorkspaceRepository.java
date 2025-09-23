package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Integer> {

    /**
     * 주어진 URL을 가진 워크스페이스가 존재하는지 확인합니다.
     *
     * @param workspaceUrl 확인할 고유 URL
     * @return 존재하면 true, 그렇지 않으면 false
     */
    boolean existsByWorkspaceUrl(String workspaceUrl);

    /**
     * 특정 사용자 ID에 속한 모든 워크스페이스 목록을 조회합니다.
     *
     * @param userId 워크스페이스를 조회할 사용자의 ID
     * @return 해당 사용자의 모든 워크스페이스 엔티티 리스트. 결과가 없으면 빈 리스트를 반환합니다.
     */
    List<Workspace> findAllByUser_UserId(Integer userId);

    /**
     * 특정 사용자의 워크스페이스 상세 정보를 조회합니다.
     * <p>
     * 워크스페이스 ID와 사용자 ID를 모두 사용하여 해당 사용자가 소유한 워크스페이스만 조회하므로,
     * 다른 사용자의 워크스페이스에 접근하는 것을 방지합니다.
     *
     * @param workspaceId 조회할 워크스페이스의 ID
     * @param userId      워크스페이스의 소유자 ID
     * @return 조건에 맞는 워크스페이스 엔티티를 Optional로 감싸서 반환합니다. 소유권이 없거나 존재하지 않으면 빈 Optional을 반환합니다.
     */
    Optional<Workspace> findByWorkspaceIdAndUser_UserId(Integer workspaceId, Integer userId);

    /**
     * ID를 기준으로 워크스페이스(Workspace) 엔티티를 조회합니다.
     * <p>
     * 이 메서드는 {@link Workspace} 엔티티에 적용된
     * {@code @SQLRestriction("is_deleted = false")} 제약 조건을 우회하기 위해 네이티브 SQL 쿼리를 사용합니다.
     * 따라서 소프트 딜리트 처리된 워크스페이스를 포함하여 모든 상태의 워크스페이스를 조회할 수 있습니다.
     * <p>
     * 주로 소프트 딜리트 트랜잭션 내에서 DB에 반영된 최종 상태(예: {@code deletedAt} 타임스탬프)를 정확히 다시 읽어와야 할 때 사용됩니다.
     *
     * @param workspaceId 조회할 워크스페이스의 ID
     * @return 조회된 Workspace 엔티티를 담은 Optional 객체. ID가 존재하지 않으면 빈 Optional을 반환합니다.
     */
    @Query(value = """
                    SELECT * 
                    FROM workspace 
                    WHERE workspace_id = :workspaceId""",
                    nativeQuery = true)
    Optional<Workspace> findByIdIncludingDeleted(@Param("workspaceId") Integer workspaceId);

    default Workspace findByIdOrThrow(Integer workspaceId, Integer userId) {
        return findByWorkspaceIdAndUser_UserId(workspaceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 워크스페이스를 찾을 수 없거나 접근 권한이 없습니다."));
    }

    boolean existsByWorkspaceIdAndUser_UserId(Integer workspaceId, Integer userId);
}
