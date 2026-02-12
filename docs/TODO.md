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

Remove this file when done.