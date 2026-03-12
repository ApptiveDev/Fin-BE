package apptive.fin.term;

import apptive.fin.auth.AuthUserDetails;
import apptive.fin.term.dto.TermResponseDto;
import apptive.fin.term.dto.UserTermRequestDto;
import apptive.fin.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TermController.class)
class TermControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TermService termService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 약관조회_API_테스트() throws Exception {

        User user = Mockito.mock(User.class);
        AuthUserDetails userDetails = new AuthUserDetails(user);

        List<TermResponseDto> response = List.of(
                new TermResponseDto(1L, "서비스 이용약관", "내용", true, false)
        );

        Mockito.when(termService.getTermsForUser(Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(get("/terms")
                        .principal(() -> "user"))
                .andExpect(status().isOk());
    }

    @Test
    void 약관동의_API_테스트() throws Exception {

        UserTermRequestDto request = new UserTermRequestDto(List.of(1L, 2L));

        mockMvc.perform(post("/terms/agree")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }
}