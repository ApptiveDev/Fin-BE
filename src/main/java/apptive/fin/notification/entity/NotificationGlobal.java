package apptive.fin.notification.entity;

import apptive.fin.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notification_global")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationGlobal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "paused_reason", length = 30)
    private PausedReason pausedReason;

    @Builder
    public NotificationGlobal(String email) {
        this.email = email;
    }

    public void pause(PausedReason reason) {
        this.enabled = false;
        this.pausedReason = reason;
    }

    public void resume() {
        this.enabled = true;
        this.pausedReason = null;
    }
}
