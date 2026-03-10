package apptive.fin.user;

import apptive.fin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_provider_account", columnNames = {"provider", "provider_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "provider", length = 50, nullable = false)
    private String provider;

    @Column(name = "provider_id", length = 255, nullable = false)
    private String providerId;

    @Enumerated
    @Column(name = "user_role", nullable = false)
    private UserRole userRole = UserRole.BEFORE_AGREED;



    @Builder
    public User(String name, String email, String provider, String providerId, UserRole userRole) {
        this.name = name;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.userRole = userRole;
    }


}
