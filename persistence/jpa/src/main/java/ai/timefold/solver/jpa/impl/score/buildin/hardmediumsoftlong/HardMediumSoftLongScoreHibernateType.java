package ai.timefold.solver.jpa.impl.score.buildin.hardmediumsoftlong;

import ai.timefold.solver.core.impl.score.buildin.HardMediumSoftLongScoreDefinition;
import ai.timefold.solver.jpa.impl.score.AbstractScoreHibernateType;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.CompositeUserType;

/**
 * @deprecated This class has been deprecated as the Hibernate 6 does not provide full backward compatibility
 *             for the {@link CompositeUserType}.
 *             The class will remain available in the OptaPlanner 8 releases to provide
 *             integration with Hibernate 5 but will be removed in OptaPlanner 9.
 *             To integrate the {@link ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore}
 *             with Hibernate 6, either use the score converter
 *             {@link ai.timefold.solver.jpa.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScoreConverter})
 *             or implement the {@link CompositeUserType} yourself.
 */
@Deprecated(forRemoval = true)
public class HardMediumSoftLongScoreHibernateType extends AbstractScoreHibernateType {

    public HardMediumSoftLongScoreHibernateType() {
        scoreDefinition = new HardMediumSoftLongScoreDefinition();
        type = StandardBasicTypes.LONG;
    }

}
