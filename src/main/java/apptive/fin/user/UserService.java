package apptive.fin.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public String getMyInfo(){
        return "user info";
    }

    public void validateEmailDuplicate(String email){

        if(userRepository.existsByEmail(email)){
            throw new IllegalArgumentException("이미 사용중인 이메일");
        }

    }

}
