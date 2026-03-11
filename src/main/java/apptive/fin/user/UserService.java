package apptive.fin.user;

import apptive.fin.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponseDto getMyInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        // ++ 유렬님 코드 참고해서 나중에 에러코드만 따로 뽑아내서 정의하기

        return new UserResponseDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getUserRole().toString()
        );

    }

    public void updateUser(Long userId, UserUpdateRequestDto request){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if(request.getEmail() != null){
            if(userRepository.existsByEmail(request.getEmail())){
                throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
            }
            user.updateEmail(request.getEmail());
        }

        if(request.getName() != null){
            user.updateName(request.getName());
        }

    }

}
