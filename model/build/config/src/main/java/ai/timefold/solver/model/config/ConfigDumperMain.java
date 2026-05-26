package ai.timefold.solver.model.config;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class ConfigDumperMain {

    public static void main(String... args) {
        Config config = ConfigProvider.getConfig();
        for (String arg : args) {
            try {
                String value = config.getValue(arg, String.class);
                System.out.println(arg + "=" + value);
            } catch (Exception e) {
                System.err.printf("Failed to get '%s' due to: %s%n", arg, e.getMessage());
            }
        }
    }
}
