# Changes to mention in release notes for 2.0.0

- [ ] Chained var removal.
- [ ] Spring 4 upgrade + changes to Jackson.
- [ ] Random -> RandomGenerator
- [ ] New rules for getters/setters.
- [ ] `webui` module removal.
- [ ] `benchmark-aggregator` split into separate module.
- [ ] `benchmark` no longer has the method to open in a browser.
- [ ] Migration recipe must mention to first run Solver migration to 1.x, and only then to 2.0.0.
- [ ] `SolverJobBuilder` consumer methods need to be migrated manually (copy from 1.x upgrade recipe); enterprise event changed as well.
- [ ] `ConstraintStreamImplType` is gone; configs, Quarkus, Spring, constraint verifier.
- [ ] Removal of PiggyBack
- [ ] Removal of VariableListener together with all related shadow variables
- [ ] Removal of old environment modes (Migration script?)
- [ ] `ConstraintConfiguration` replaced by `ConstraintWeightOverrides` (copy from 1.x upgrade recipe).
- [ ] `ConstraintRef.of()` now only accepts name, not package and name. (Migration script!)
- [ ] Custom scores are no longer possible. (They weren't even before, really.)
- [ ] ai.timefold.solver.api.score.buildin.* -> ai.timefold.solver.api.score
- [ ] ai.timefold.solver.jpa.api.score.buildin.* -> ai.timefold.solver.jpa.api.score 
- [ ] ai.timefold.solver.jackson.api.score.buildin.* -> ai.timefold.solver.jackson.api.score
- [ ] ai.timefold.solver.jaxb.api.score.buildin.* -> ai.timefold.solver.jaxb.api.score
- [ ] Bendable constructors now long[] instead of int[].
- [ ] penalizeLong/rewardLong/impactLong are no more.
- [ ] Value ranges and move selectors (not public API) no longer have isCountable().
- [ ] The solver now implicitly trusts equals() on objects, such as entities and values.
- [ ] BestSolutionChangedEvent now an interface. (All constructors were deprecated anyway.)
- [ ] ProblemFactChange -> ProblemChange, migration script?
- [ ] PinningFilter is gone, so is strengths and difficulties, and nullable.
- [ ] PlanningId moves from domain.lookup to domain.common.
- [ ] domain.lookup package is gone, so is lookup from PlanningSolution.
- [ ] lookups no longer accept null values.
- [ ] `DomainAccessType` is gone, code uses GIZMO when possible

Remove this file when done.