package ai.timefold.solver.jpa.impl.score.buildin.hardmediumsoftbigdecimal;

import ai.timefold.solver.core.impl.score.buildin.HardMediumSoftBigDecimalScoreDefinition;
import ai.timefold.solver.jpa.impl.score.AbstractScoreHibernateType;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.CompositeUserType;

/**
 * @deprecated This class has been deprecated as the Hibernate 6 does not provide full backward compatibility
 *             for the {@link CompositeUserType}.
 *             The class will remain available in the OptaPlanner 8 releases to provide
 *             integration with Hibernate 5 but will be removed in OptaPlanner 9.
 *             To integrate the
 *             {@link ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore}
 *             with Hibernate 6, either use the score converter
 *             {@link ai.timefold.solver.jpa.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreConverter})
 *             or implement the {@link CompositeUserType} yourself.
 */
@Deprecated(forRemoval = true)
public class HardMediumSoftBigDecimalScoreHibernateType extends AbstractScoreHibernateType {

    public HardMediumSoftBigDecimalScoreHibernateType() {
        scoreDefinition = new HardMediumSoftBigDecimalScoreDefinition();
        type = StandardBasicTypes.BIG_DECIMAL;
    }

}
