package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.Recipient;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RecipientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RecipientRepository recipientRepository;

    private Workspace testWorkspace;
    private Workspace anotherWorkspace;
    private Recipient recipient1;
    private Recipient recipient2;
    private Recipient recipient3;

    @BeforeEach
    void setUp() {
        // Given: 테스트 데이터 설정
        User testUser = User.builder()
                .userName("테스트유저")
                .userEmail("test@example.com")
                .userNumber("010-1234-5678")
                .build();
        entityManager.persist(testUser);

        testWorkspace = Workspace.builder()
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl("test-url")
                .representerName("테스트대표")
                .representerPhoneNumber("010-0000-0000")
                .companyName("테스트회사")
                .user(testUser)
                .build();
        entityManager.persist(testWorkspace);

        anotherWorkspace = Workspace.builder()
                .workspaceName("다른 워크스페이스")
                .workspaceUrl("another-url")
                .representerName("다른 대표")
                .representerPhoneNumber("010-0000-0000")
                .companyName("다른 회사")
                .user(testUser)
                .build();
        entityManager.persist(anotherWorkspace);

        recipient1 = Recipient.builder()
                .recipientName("홍길동")
                .recipientPhoneNumber("010-1111-2222")
                .workspace(testWorkspace)
                .build();
        recipient2 = Recipient.builder()
                .recipientName("임꺽정")
                .recipientPhoneNumber("010-2222-3333")
                .workspace(testWorkspace)
                .build();
        recipient3 = Recipient.builder()
                .recipientName("김철수")
                .recipientPhoneNumber("010-3333-4444")
                .workspace(anotherWorkspace)
                .build();
        entityManager.persist(recipient1);
        entityManager.persist(recipient2);
        entityManager.persist(recipient3);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("특정 워크스페이스가 소유한 모든 수신자 목록 페이징 조회 테스트")
    void findAllByWorkspace_WorkspaceId_Paging_Test() {
        // given
        // 1. 페이징 요청 정보를 담은 Pageable 객체를 생성합니다. 0번째 페이지, 5개씩, 이름 오름차순 정렬을 요청합니다.
        Pageable pageable = PageRequest.of(0, 5, Sort.by("recipientName").ascending());

        // when
        // 1. 테스트하려는 페이징 쿼리 메서드를 호출합니다.
        Page<Recipient> recipientPage =
                recipientRepository.findAllByWorkspace_WorkspaceId(testWorkspace.getWorkspaceId(), pageable);

        // then
        // 1. Page 객체의 주요 정보들을 검증합니다.
        assertThat(recipientPage).isNotNull(); // Page 객체가 null이 아닌지 확인
        assertThat(recipientPage.getTotalElements()).isEqualTo(2); // 전체 데이터 개수 검증
        assertThat(recipientPage.getTotalPages()).isEqualTo(1); // 전체 페이지 수 검증
        assertThat(recipientPage.getContent().size()).isEqualTo(2); // 현재 페이지의 데이터 수 검증

        // 2. 현재 페이지의 내용(content)을 검증합니다. 이름 오름차순으로 정렬되었는지 확인합니다.
        assertThat(recipientPage.getContent()).extracting(Recipient::getRecipientName)
                .containsExactly("임꺽정", "홍길동");
    }

    @Test
    @DisplayName("수신자 ID와 워크스페이스 ID로 수신자 조회 성공 테스트")
    void findByRecipientIdAndWorkspace_WorkspaceId_Success_Test() {
        // given
        // 1. 테스트 데이터 준비: BeforeEach에서 이미 "홍길동" 수신자가 testWorkspace에 저장되어 있습니다.
        //    정확한 ID를 알기 위해 먼저 해당 수신자를 조회합니다.
        Recipient targetRecipient = entityManager.getEntityManager()
                .createQuery("SELECT r FROM Recipient r WHERE r.recipientName = :name", Recipient.class)
                .setParameter("name", "홍길동")
                .getSingleResult();

        // when
        // 1. 테스트하려는 쿼리 메소드를 올바른 ID로 호출합니다.
        java.util.Optional<Recipient> foundRecipientOpt =
                recipientRepository.findByRecipientIdAndWorkspace_WorkspaceId(
                        targetRecipient.getRecipientId(),
                        testWorkspace.getWorkspaceId()
                );

        // then
        // 1. Optional 객체가 비어있지 않은지 확인합니다. (조회 성공)
        assertThat(foundRecipientOpt).isPresent();
        // 2. 조회된 Recipient 객체의 ID와 이름이 예상과 일치하는지 확인합니다.
        assertThat(foundRecipientOpt.get().getRecipientId()).isEqualTo(targetRecipient.getRecipientId());
        assertThat(foundRecipientOpt.get().getRecipientName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("수신자 ID와 워크스페이스 ID로 수신자 조회 실패 테스트 - 워크스페이스 불일치")
    void findByRecipientIdAndWorkspace_WorkspaceId_Fail_WorkspaceMismatch_Test() {
        // given
        // 1. 테스트 데이터 준비: "홍길동" 수신자의 ID를 가져옵니다.
        Recipient targetRecipient = entityManager.getEntityManager()
                .createQuery("SELECT r FROM Recipient r WHERE r.recipientName = :name", Recipient.class)
                .setParameter("name", "홍길동")
                .getSingleResult();

        // when
        // 1. 테스트하려는 쿼리 메소드를 호출하되,
        //    "홍길동" 수신자가 속하지 않은 'anotherWorkspace'의 ID로 조회합니다.
        java.util.Optional<Recipient> foundRecipientOpt =
                recipientRepository.findByRecipientIdAndWorkspace_WorkspaceId(
                        targetRecipient.getRecipientId(),
                        anotherWorkspace.getWorkspaceId() // 일부러 다른 워크스페이스 ID 사용
                );

        // then
        // 1. Optional 객체가 비어있는지 확인합니다. (조회 실패)
        assertThat(foundRecipientOpt).isNotPresent();
    }

    @Test
    @DisplayName("워크스페이스, 이름, 전화번호로 수신자 존재 여부 확인 성공 테스트")
    void existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber_Success_Test() {
        // given
        // 1. 테스트 데이터는 @BeforeEach 에서 이미 설정됨 ("홍길동", "010-1111-2222", testWorkspace)

        // when
        // 1. 존재하는 데이터로 존재 여부를 확인하는 메소드를 호출합니다.
        boolean exists = recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber(
                testWorkspace, "홍길동", "010-1111-2222");

        // then
        // 1. 결과가 true인지 검증합니다.
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("워크스페이스, 이름, 전화번호로 수신자 존재 여부 확인 실패 테스트")
    void existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber_Fail_Test() {
        // given
        // 1. 테스트 데이터는 @BeforeEach 에서 이미 설정됨

        // when
        // 1. 존재하지 않는 이름으로 조회
        boolean existsByName = recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber(
                testWorkspace, "없는이름", "010-1111-2222");
        // 2. 존재하지 않는 전화번호로 조회
        boolean existsByPhoneNumber = recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber(
                testWorkspace, "홍길동", "010-9999-9999");
        // 3. 다른 워크스페이스로 조회
        boolean existsByWorkspace = recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumber(
                anotherWorkspace, "홍길동", "010-1111-2222");

        // then
        // 1. 모든 결과가 false인지 검증합니다.
        assertThat(existsByName).isFalse();
        assertThat(existsByPhoneNumber).isFalse();
        assertThat(existsByWorkspace).isFalse();
    }

    @Test
    @DisplayName("수정 시 자기 자신을 제외한 중복 수신자 존재 여부 확인 성공 테스트")
    void existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot_Success_NoDuplicate_Test() {
        // given
        // 1. 테스트 데이터는 @BeforeEach 에서 이미 설정됨 ("홍길동", "010-1111-2222", testWorkspace)
        // 2. 추가로 다른 수신자를 저장합니다.
        Recipient anotherRecipient = recipientRepository.save(Recipient.builder()
                .recipientName("김철수")
                .recipientPhoneNumber("010-3333-3333")
                .workspace(testWorkspace)
                .build());

        // when
        // 1. 자기 자신(recipient1)을 제외하고, 존재하지 않는 이름과 번호 조합으로 중복 확인
        boolean exists = recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot(
                testWorkspace, "박영수", "010-4444-4444", recipient1.getRecipientId());

        // then
        // 1. 결과가 false인지 검증합니다. (중복이 없음)
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("수정 시 자기 자신을 제외한 중복 수신자 존재 여부 확인 실패 테스트 - 다른 수신자와 중복")
    void existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot_Fail_DuplicateWithOther_Test() {
        // given
        // 1. 테스트 데이터는 @BeforeEach 에서 이미 설정됨 ("홍길동", "010-1111-2222", testWorkspace)
        // 2. 추가로 다른 수신자를 저장합니다.
        Recipient anotherRecipient = recipientRepository.save(Recipient.builder()
                .recipientName("김철수")
                .recipientPhoneNumber("010-3333-3333")
                .workspace(testWorkspace)
                .build());

        // when
        // 1. 자기 자신(recipient1)을 제외하고, 다른 수신자(anotherRecipient)와 동일한 이름과 번호로 중복 확인
        boolean exists = recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot(
                testWorkspace, "김철수", "010-3333-3333", recipient1.getRecipientId());

        // then
        // 1. 결과가 true인지 검증합니다. (다른 수신자와 중복됨)
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("수정 시 자기 자신을 제외한 중복 수신자 존재 여부 확인 성공 테스트 - 자기 자신과는 중복 허용")
    void existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot_Success_SelfExcluded_Test() {
        // given
        // 1. 테스트 데이터는 @BeforeEach 에서 이미 설정됨 ("홍길동", "010-1111-2222", testWorkspace)

        // when
        // 1. 자기 자신(recipient1)을 제외하고, 자기 자신과 동일한 이름과 번호로 중복 확인
        boolean exists = recipientRepository.existsByWorkspaceAndRecipientNameAndRecipientPhoneNumberAndRecipientIdNot(
                testWorkspace, "홍길동", "010-1111-2222", recipient1.getRecipientId());

        // then
        // 1. 결과가 false인지 검증합니다. (자기 자신은 제외되므로 중복이 아님)
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("소프트 딜리트된 수신자 포함 조회 성공 테스트")
    void findByIdIncludingDeleted_Success_ActiveRecipient_Test() {
        // given
        // 1. 테스트 데이터는 @BeforeEach 에서 이미 설정됨 (활성 상태의 recipient1)

        // when
        // 1. 네이티브 쿼리로 활성 상태의 수신자를 조회합니다.
        java.util.Optional<Recipient> foundRecipientOpt =
                recipientRepository.findByIdIncludingDeleted(recipient1.getRecipientId());

        // then
        // 1. Optional 객체가 비어있지 않은지 확인합니다. (조회 성공)
        assertThat(foundRecipientOpt).isPresent();
        // 2. 조회된 수신자의 정보가 올바른지 확인합니다.
        assertThat(foundRecipientOpt.get().getRecipientName()).isEqualTo("홍길동");
        assertThat(foundRecipientOpt.get().getIsDeleted()).isFalse();
        assertThat(foundRecipientOpt.get().getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("소프트 딜리트된 수신자 포함 조회 성공 테스트 - 삭제된 수신자")
    void findByIdIncludingDeleted_Success_DeletedRecipient_Test() {
        // given
        // 1. recipient1을 소프트 딜리트 처리합니다.
        recipient1.softDelete();
        recipientRepository.save(recipient1);
        entityManager.flush();

        // when
        // 1. 네이티브 쿼리로 소프트 딜리트된 수신자를 조회합니다.
        Optional<Recipient> foundRecipientOpt =
                recipientRepository.findByIdIncludingDeleted(recipient1.getRecipientId());

        // then
        // 1. Optional 객체가 비어있지 않은지 확인합니다. (조회 성공)
        assertThat(foundRecipientOpt).isPresent();
        // 2. 조회된 수신자가 삭제된 상태인지 확인합니다.
        assertThat(foundRecipientOpt.get().getRecipientName()).isEqualTo("홍길동");
        assertThat(foundRecipientOpt.get().getIsDeleted()).isTrue();
        assertThat(foundRecipientOpt.get().getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("소프트 딜리트된 수신자 포함 조회 실패 테스트 - 존재하지 않는 ID")
    void findByIdIncludingDeleted_Fail_NonExistentId_Test() {
        // given
        // 1. 존재하지 않는 수신자 ID를 준비합니다.
        Integer nonExistentId = 9999;

        // when
        // 1. 존재하지 않는 ID로 네이티브 쿼리 조회를 시도합니다.
        Optional<Recipient> foundRecipientOpt =
                recipientRepository.findByIdIncludingDeleted(nonExistentId);

        // then
        // 1. Optional 객체가 비어있는지 확인합니다. (조회 실패)
        assertThat(foundRecipientOpt).isNotPresent();
    }
}
