package apptive.fin.auth.oauth.touserinfo;

public interface OAuth2UserInfo {
    String getProvider();
    String getName();
    String getEmail();
    String getProviderId();
}