package ai.timefold.solver.tools.maven.client;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public record PlatformIdentityInfo(String user, Set<String> scopes, Set<String> accountIds, Set<UUID> tenants,
        ConfigurationInfo config) {

    private static final Set<String> REQUIRED_SCOPES = Set.of("registered-model:create", "registered-model:update");

    public boolean hasPushAccessRights() {
        return scopes().stream().anyMatch(scope -> REQUIRED_SCOPES.contains(scope));
    }

    public boolean hasAccessToAccountId(String account) {
        if (accountIds == null || accountIds.isEmpty()) {
            return false;
        }

        return accountIds().contains(account);
    }
}
