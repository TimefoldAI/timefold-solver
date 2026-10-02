package ai.timefold.solver.tools.maven.client;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The identity behind the personal access token, as reported by the platform's {@code aboutme} endpoint. The namespaces
 * the token is associated with are being migrated by Timefold Platform from {@code accountIds} to {@code namespaces},
 * so both field names are accepted and either one may be missing from the response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_NULL)
public record PlatformIdentityInfo(String user, Set<String> scopes, @JsonAlias("accountIds") Set<String> namespaces,
        Set<UUID> tenants, ConfigurationInfo config) {

    private static final Set<String> REQUIRED_SCOPES = Set.of("registered-model:create", "registered-model:update");

    public PlatformIdentityInfo {
        // The platform may leave these out of the response, so normalize them and keep the rest of the plugin null free.
        scopes = scopes == null ? Set.of() : scopes;
        namespaces = namespaces == null ? Set.of() : namespaces;
    }

    public boolean hasPushAccessRights() {
        return scopes().stream().anyMatch(REQUIRED_SCOPES::contains);
    }

    public boolean hasAccessToNamespace(String namespace) {
        return namespaces().contains(namespace);
    }
}
