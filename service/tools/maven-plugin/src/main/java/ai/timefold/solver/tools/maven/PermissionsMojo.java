package ai.timefold.solver.tools.maven;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import ai.timefold.solver.tools.maven.client.PlatformIdentityInfo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Goal that reports the permissions/scopes the configured Personal Access Token has on Timefold Platform.
 */
@Mojo(name = "permissions")
public class PermissionsMojo extends AbstractPlatformModelMojo {

    private static final String NONE = "(none)";

    @Override
    public void execute() throws MojoExecutionException {
        PlatformIdentityInfo info = fetchPlatformIdentityInfo(false);
        if (info == null) {
            throw new MojoExecutionException("Timefold Platform did not return any information for the configured token");
        }
        report(info);
    }

    private void report(PlatformIdentityInfo info) {
        getLog().info("Timefold Platform - configured token");
        getLog().info("  Platform   : " + getPlatformUrl());
        getLog().info("  User       : " + orNone(info.user()));
        getLog().info("  Scopes     : " + joinStrings(info.scopes()));
        getLog().info("  Tenants    : " + joinUuids(info.tenants()));
        getLog().info("  Namespaces : " + joinStrings(info.accountIds()));

        List<String> selectedTenants = getTenants();
        if (selectedTenants != null && !selectedTenants.isEmpty()) {
            getLog().info("  Selected tenant : " + selectedTenants.getFirst());
        }

        if (info.hasPushAccessRights()) {
            getLog().info("  Deploy     : OK - token can register/update models (push access granted)");
        } else {
            getLog().warn("  Deploy     : token CANNOT register/update models - missing "
                    + "'registered-model:create' or 'registered-model:update' scope");
        }
    }

    private static String orNone(String value) {
        return value == null || value.isBlank() ? NONE : value;
    }

    private static String joinStrings(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return NONE;
        }
        return String.join(", ", new TreeSet<>(values));
    }

    private static String joinUuids(Set<UUID> values) {
        if (values == null || values.isEmpty()) {
            return NONE;
        }
        TreeSet<String> sorted = new TreeSet<>();
        for (UUID value : values) {
            sorted.add(value.toString());
        }
        return String.join(", ", sorted);
    }
}
