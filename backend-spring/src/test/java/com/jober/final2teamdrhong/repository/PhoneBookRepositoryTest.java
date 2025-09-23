package com.jober.final2teamdrhong.repository;

import com.jober.final2teamdrhong.entity.PhoneBook;
import com.jober.final2teamdrhong.entity.User;
import com.jober.final2teamdrhong.entity.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PhoneBookRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PhoneBookRepository phoneBookRepository;

    private Workspace testWorkspace;
    private Workspace anotherWorkspace;
    private PhoneBook testPhoneBook;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .userName("user")
                .userEmail("user@test.com")
                .build();
        entityManager.persist(user);

        testWorkspace = Workspace.builder()
                .workspaceName("테스트 워크스페이스")
                .workspaceUrl("test-url")
                .representerName("테스트대표")
                .representerPhoneNumber("010-0000-0000")
                .companyName("테스트회사")
                .user(user)
                .build();
        entityManager.persist(testWorkspace);

        anotherWorkspace = Workspace.builder()
                .workspaceName("테스트 워크스페이스2")
                .workspaceUrl("test-url-2")
                .representerName("테스트대표")
                .representerPhoneNumber("010-0000-0000")
                .companyName("테스트회사")
                .user(user)
                .build();
        entityManager.persist(anotherWorkspace);

        testPhoneBook = PhoneBook.builder()
                .phoneBookName("test-phonebook")
                .workspace(testWorkspace)
                .build();
        entityManager.persist(testPhoneBook);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("주소록 ID와 워크스페이스 ID로 주소록 조회 성공 테스트")
    void findByPhoneBookIdAndWorkspace_WorkspaceId_Success_Test() {
        // given
        // 1. @BeforeEach에서 testPhoneBook이 testWorkspace에 속하도록 설정되어 있습니다.

        // when
        // 1. 테스트 대상 메서드를 올바른 주소록 ID와 워크스페이스 ID로 호출합니다.
        Optional<PhoneBook> foundPhoneBookOpt = phoneBookRepository.findByPhoneBookIdAndWorkspace_WorkspaceId(
                testPhoneBook.getPhoneBookId(), testWorkspace.getWorkspaceId());

        // then
        // 1. Optional 객체가 비어있지 않은지(조회 성공) 확인합니다.
        assertThat(foundPhoneBookOpt).isPresent();
        // 2. 조회된 주소록의 ID가 예상과 일치하는지 확인합니다.
        assertThat(foundPhoneBookOpt.get().getPhoneBookId()).isEqualTo(testPhoneBook.getPhoneBookId());
        // 3. 조회된 주소록이 속한 워크스페이스의 ID가 예상과 일치하는지 확인합니다.
        assertThat(foundPhoneBookOpt.get().getWorkspace().getWorkspaceId()).isEqualTo(testWorkspace.getWorkspaceId());
    }

    @Test
    @DisplayName("주소록 ID와 워크스페이스 ID로 주소록 조회 실패 테스트 - 워크스페이스 불일치")
    void findByPhoneBookIdAndWorkspace_WorkspaceId_Fail_WorkspaceMismatch_Test() {
        // given
        // 1. @BeforeEach에서 testPhoneBook은 testWorkspace에 속해 있습니다.

        // when
        // 1. 테스트 대상 메서드를 호출하되, 다른 워크스페이스 ID(anotherWorkspace)를 사용합니다.
        Optional<PhoneBook> foundPhoneBookOpt = phoneBookRepository.findByPhoneBookIdAndWorkspace_WorkspaceId(
                testPhoneBook.getPhoneBookId(), anotherWorkspace.getWorkspaceId());

        // then
        // 1. Optional 객체가 비어있는지(조회 실패) 확인합니다.
        assertThat(foundPhoneBookOpt).isNotPresent();
    }

    @Test
    @DisplayName("주소록 ID와 워크스페이스 ID로 주소록 조회 실패 테스트 - 존재하지 않는 주소록 ID")
    void findByPhoneBookIdAndWorkspace_WorkspaceId_Fail_PhoneBookNotFound_Test() {
        // given
        // 1. DB에 존재하지 않을 임의의 ID를 설정합니다.
        Integer nonExistentPhoneBookId = -1;

        // when
        // 1. 존재하지 않는 주소록 ID로 메서드를 호출합니다.
        Optional<PhoneBook> foundPhoneBookOpt = phoneBookRepository.findByPhoneBookIdAndWorkspace_WorkspaceId(
                nonExistentPhoneBookId, testWorkspace.getWorkspaceId());

        // then
        // 1. Optional 객체가 비어있는지(조회 실패) 확인합니다.
        assertThat(foundPhoneBookOpt).isNotPresent();
    }
}
