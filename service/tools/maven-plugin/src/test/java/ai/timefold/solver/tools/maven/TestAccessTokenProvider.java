package ai.timefold.solver.tools.maven;

public class TestAccessTokenProvider extends AccessTokenProvider {

    private final String token;

    public TestAccessTokenProvider(String token) {
        this.token = token;
    }

    @Override
    public String getAccessToken() {
        return token;
    }

}
