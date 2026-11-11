package apptive.fin.auth.oauth.userinfo;

public interface OAuth2UserInfo {
    String getProvider();
    String getName();
    String getEmail();
    String getProviderId();

    default boolean isEmailVerified() {
        return false;
    }
}