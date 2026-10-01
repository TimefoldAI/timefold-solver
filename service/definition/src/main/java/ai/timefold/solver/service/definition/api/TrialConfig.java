package ai.timefold.solver.service.definition.api;

/**
 * Configuration of trial use of the model
 *
 * <ul>
 * <li>trialDuration - duration in days of the trial period model can be used</li>
 * <li>maxExtensions - max number of extensions trial can have</li>
 * <li>extensionDuration - duration in days indicating how long trial can be extended for</li>
 * </ul>
 */
public record TrialConfig(Integer trialDuration, Integer maxExtensions, Integer extensionDuration) {

}
