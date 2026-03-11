package apptive.fin.auth;

import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class AuthUserDetails extends User {
    private final apptive.fin.user.User user;

    public AuthUserDetails(apptive.fin.user.User user) {
        super(user.getOAuthIdentifier(), "",
                List.of(new SimpleGrantedAuthority(user.getUserRole().toString()))
        );
        this.user = user;
    }
}
