package apptive.fin.term;

import apptive.fin.term.dto.TermResponseDto;
import apptive.fin.term.dto.UserTermRequestDto;
import apptive.fin.term.entity.Term;
import apptive.fin.term.entity.UserTerm;
import apptive.fin.term.repository.TermRepository;
import apptive.fin.term.repository.UserTermRepository;
import apptive.fin.term.service.TermService;
import apptive.fin.user.entity.User;
import apptive.fin.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class TermServiceTest {

    @Autowired
    private TermService termService;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserTermRepository userTermRepository;

    @Test
    @DisplayName("Term 생성 테스트")
    void createTermTest() {

        Term term = Term.builder()
                .title("서비스 이용약관")
                .content("약관 내용")
                .isRequired(true)
                .build();

        Term saved = termRepository.save(term);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("서비스 이용약관");
    }

    @Test
    @DisplayName("User가 약관에 동의하면 UserTerm이 생성된다")
    void agreeTermsTest() {

        // user 생성
        User user = userRepository.save(
                User.builder()
                        .email("test@test.com")
                        .name("tester")
                        .provider("google")
                        .providerId("1234")
                        .build()
        );

        // term 생성
        Term term = termRepository.save(
                Term.builder()
                        .title("개인정보 처리방침")
                        .content("내용")
                        .isRequired(true)
                        .build()
        );

        UserTermRequestDto request = new UserTermRequestDto(List.of(term.getId()));

        termService.agreeTerms(user, request);

        List<UserTerm> result = userTermRepository.findAllByUser(user);

        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).isAgreed()).isTrue();
    }

    @Test
    @DisplayName("유저 기준 약관 조회 테스트")
    void getTermsForUserTest() {

        User user = userRepository.save(
                User.builder()
                        .email("test@test.com")
                        .name("tester")
                        .provider("google")
                        .providerId("9999")
                        .build()
        );

        Term term = termRepository.save(
                Term.builder()
                        .title("마케팅 수신 동의")
                        .content("내용")
                        .isRequired(false)
                        .build()
        );

        List<TermResponseDto> terms = termService.getTermsForUser(user);

        assertThat(terms).isNotEmpty();
        assertThat(terms.get(0).title()).isEqualTo("마케팅 수신 동의");
    }
}