package ai.timefold.solver.model.quarkus.deployment.graalvm.validator;

import com.networknt.schema.ValidationContext;
import com.networknt.schema.regex.RegularExpression;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Replaces com.networknt.schema.regex.RegularExpression to avoid checking of optional dependency that is
 * brought by JoniRegularExpression class
 */
@TargetClass(RegularExpression.class)
final class Target_com_networknt_schema_regex_RegularExpression {

    @Substitute
    static RegularExpression compile(String regex, ValidationContext validationContext) {
        if (null == regex) {
            return new RegularExpression() {

                @Override
                public boolean matches(String value) {
                    return true;
                }
            };
        }
        return new JDKRegularExpression(regex);
    }
}
