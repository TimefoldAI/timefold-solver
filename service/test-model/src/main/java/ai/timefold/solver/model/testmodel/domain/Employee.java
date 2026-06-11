package ai.timefold.solver.service.testmodel.domain;

import java.util.Set;

public class Employee {

    private String id;

    private Set<Skill> skills;

    public Employee() {
    }

    public Employee(String id, Set<Skill> skills) {
        this.id = id;
        this.skills = skills;
    }

    public Employee(String id, Skill... skills) {
        this.id = id;
        this.skills = Set.of(skills);
    }

    public String getId() {
        return id;
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public boolean isQualified(Shift shift) {
        return skills.contains(shift.getRequiredSkill());
    }
}
