package ai.timefold.solver.jpa.impl.score.buildin.simple;

import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;
import ai.timefold.solver.jpa.impl.score.AbstractScoreHibernateType;

import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.CompositeUserType;

/**
 * @deprecated This class has been deprecated as the Hibernate 6 does not provide full backward compatibility
 *             for the {@link CompositeUserType}.
 *             The class will remain available in the OptaPlanner 8 releases to provide
 *             integration with Hibernate 5 but will be removed in OptaPlanner 9.
 *             To integrate the {@link ai.timefold.solver.core.api.score.buildin.simple.SimpleScore}
 *             with Hibernate 6, either use the score converter
 *             {@link ai.timefold.solver.jpa.api.score.buildin.simple.SimpleScoreConverter})
 *             or implement the {@link CompositeUserType} yourself.
 */
@Deprecated(forRemoval = true)
public class SimpleScoreHibernateType extends AbstractScoreHibernateType {

    public SimpleScoreHibernateType() {
        scoreDefinition = new SimpleScoreDefinition();
        type = StandardBasicTypes.INTEGER;
    }

}
