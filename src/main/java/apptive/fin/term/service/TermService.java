package apptive.fin.term.service;

import apptive.fin.term.entity.Term;
import apptive.fin.term.repository.TermRepository;
import apptive.fin.term.entity.UserTerm;
import apptive.fin.term.repository.UserTermRepository;
import apptive.fin.term.dto.TermResponseDto;
import apptive.fin.term.dto.UserTermRequestDto;
import apptive.fin.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository termRepository;
    private final UserTermRepository userTermRepository;

    public List<TermResponseDto> getTermsForUser(User user) {
        List<UserTerm> userTerms = userTermRepository.findAllByUser(user);

        return termRepository.findAll()
                .stream()
                .map(term -> {
                    boolean agreed = userTerms.stream()
                            .anyMatch(ut -> ut.getTerm().getId().equals(term.getId()) && ut.isAgreed());
                    return new TermResponseDto(
                            term.getId(),
                            term.getTitle(),
                            term.getContent(),
                            term.isRequired(),
                            agreed
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void agreeTerms(User user, UserTermRequestDto request) {
        List<Term> termsToAgree = termRepository.findAllById(request.termIds());

        for (Term term : termsToAgree) {
            UserTerm userTerm = userTermRepository.findByUserAndTerm(user, term)
                    .orElse(UserTerm.builder()
                            .user(user)
                            .term(term)
                            .agreed(false)
                            .agreedAt(null)
                            .build()
                    );

            userTerm.agree();
            userTermRepository.save(userTerm);
        }
    }

}