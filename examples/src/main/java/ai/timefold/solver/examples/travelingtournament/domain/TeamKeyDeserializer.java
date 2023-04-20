package ai.timefold.solver.examples.travelingtournament.domain;

import ai.timefold.solver.examples.common.persistence.jackson.AbstractKeyDeserializer;

final class TeamKeyDeserializer extends AbstractKeyDeserializer<Team> {

    public TeamKeyDeserializer() {
        super(Team.class);
    }

    @Override
    protected Team createInstance(long id) {
        return new Team(id);
    }
}
