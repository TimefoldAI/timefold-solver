package ai.timefold.solver.tools.maven;

public class AccessTokenProvider {

    public String getAccessToken() {
        return System.getenv("TIMEFOLD_PAT");
    }
}
