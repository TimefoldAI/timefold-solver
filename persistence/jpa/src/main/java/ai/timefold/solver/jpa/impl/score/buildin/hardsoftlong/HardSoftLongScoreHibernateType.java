package ai.timefold.solver.jpa.impl.score.buildin.hardsoftlong;

import ai.timefold.solver.core.impl.score.buildin.HardSoftLongScoreDefinition;
import ai.timefold.solver.jpa.impl.score.AbstractScoreHibernateType;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.CompositeUserType;

/**
 * @deprecated This class has been deprecated as the Hibernate 6 does not provide full backward compatibility
 *             for the {@link CompositeUserType}.
 *             The class will remain available in the Timefold 8 releases to provide
 *             integration with Hibernate 5 but will be removed in Timefold 9.
 *             To integrate the {@link ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore}
 *             with Hibernate 6, either use the score converter
 *             {@link ai.timefold.solver.jpa.api.score.buildin.hardsoftlong.HardSoftLongScoreConverter})
 *             or implement the {@link CompositeUserType} yourself.
 */
@Deprecated(forRemoval = true)
public class HardSoftLongScoreHibernateType extends AbstractScoreHibernateType {

    public HardSoftLongScoreHibernateType() {
        scoreDefinition = new HardSoftLongScoreDefinition();
        type = StandardBasicTypes.LONG;
    }

}
