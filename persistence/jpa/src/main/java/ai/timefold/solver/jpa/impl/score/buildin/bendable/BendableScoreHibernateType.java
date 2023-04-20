package ai.timefold.solver.jpa.impl.score.buildin.bendable;

import java.util.Properties;

import ai.timefold.solver.core.impl.score.buildin.BendableScoreDefinition;
import ai.timefold.solver.jpa.impl.score.AbstractScoreHibernateType;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.ParameterizedType;

/**
 * @deprecated This class has been deprecated as the Hibernate 6 does not provide full backward compatibility
 *             for the {@link CompositeUserType}.
 *             The class will remain available in the Timefold 8 releases to provide
 *             integration with Hibernate 5 but will be removed in Timefold 9.
 *             To integrate the {@link ai.timefold.solver.core.api.score.buildin.bendable.BendableScore} with Hibernate 6,
 *             either use the score converter
 *             {@link ai.timefold.solver.jpa.api.score.buildin.bendable.BendableScoreConverter})
 *             or implement the {@link CompositeUserType} yourself.
 */
@Deprecated(forRemoval = true)
public class BendableScoreHibernateType extends AbstractScoreHibernateType implements ParameterizedType {

    @Override
    public void setParameterValues(Properties parameterMap) {
        int hardLevelsSize = extractIntParameter(parameterMap, "hardLevelsSize");
        int softLevelsSize = extractIntParameter(parameterMap, "softLevelsSize");
        scoreDefinition = new BendableScoreDefinition(hardLevelsSize, softLevelsSize);
        type = StandardBasicTypes.INTEGER;
    }

}
