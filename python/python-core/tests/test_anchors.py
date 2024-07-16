# import timefold.solver
# import timefold.solver.score
# import timefold.solver.config
# import timefold.solver.constraint
# from timefold.solver.types import PlanningVariableGraphType
#
#
# @timefold.solver.problem_fact
# class ChainedObject:
#     pass
#
#
# @timefold.solver.problem_fact
# class ChainedAnchor(ChainedObject):
#     def __init__(self, code):
#         self.code = code
#
#
# @timefold.solver.planning_entity
# class ChainedEntity(ChainedObject):
#     def __init__(self, code, value=None, anchor=None):
#         self.code = code
#         self.value = value
#         self.anchor = anchor
#
#     @timefold.solver.planning_variable(ChainedObject, value_range_provider_refs=['chained_anchor_range',
#                                                                         'chained_entity_range'],
#                               graph_type=PlanningVariableGraphType.CHAINED)
#     def get_value(self):
#         return self.value
#
#     def set_value(self, value):
#         self.value = value
#
#     @timefold.solver.anchor_shadow_variable(ChainedAnchor, source_variable_name='value')
#     def get_anchor(self):
#         return self.anchor
#
#     def set_anchor(self, anchor):
#         self.anchor = anchor
#
#     def __str__(self):
#         return f'ChainedEntity(code={self.code}, value={self.value}, anchor={self.anchor})'
#
#
# @timefold.solver.planning_solution
# class ChainedSolution:
#     def __init__(self, anchors, entities, score=None):
#         self.anchors = anchors
#         self.entities = entities
#         self.score = score
#
#     @timefold.solver.problem_fact_collection_property(ChainedAnchor)
#     @timefold.solver.value_range_provider('chained_anchor_range')
#     def get_anchors(self):
#         return self.anchors
#
#     @timefold.solver.planning_entity_collection_property(ChainedEntity)
#     @timefold.solver.value_range_provider('chained_entity_range')
#     def get_entities(self):
#         return self.entities
#
#     @timefold.solver.planning_score(timefold.solver.score.SimpleScore)
#     def get_score(self):
#         return self.score
#
#     def set_score(self, score):
#         self.score = score
#
#
# @timefold.solver.constraint_provider
# def chained_constraints(constraint_factory: timefold.solver.constraint.ConstraintFactory):
#     return [
#         constraint_factory.for_each(ChainedEntity)
#                           .group_by(lambda entity: entity.anchor, timefold.solver.constraint.ConstraintCollectors.count())
#                           .reward('Maximize chain length', timefold.solver.score.SimpleScore.ONE,
#                                   lambda anchor, count: count * count)
#     ]
#
#
# def test_chained():
#     termination = timefold.solver.config.solver.termination.TerminationConfig()
#     termination.setBestScoreLimit('9')
#     solver_config = timefold.solver.config.solver.SolverConfig() \
#         .withSolutionClass(ChainedSolution) \
#         .withEntityClasses(ChainedEntity) \
#         .withConstraintProviderClass(chained_constraints) \
#         .withTerminationConfig(termination)
#     solver = timefold.solver.solver_factory_create(solver_config).buildSolver()
#     solution = solver.solve(ChainedSolution(
#         [
#             ChainedAnchor('A'),
#             ChainedAnchor('B'),
#             ChainedAnchor('C')
#         ],
#         [
#             ChainedEntity('1'),
#             ChainedEntity('2'),
#             ChainedEntity('3'),
#         ]
#     ))
#     assert solution.score.score == 9
#     anchor = solution.entities[0].anchor
#     assert anchor is not None
#     anchor_value_count = 0
#     for entity in solution.entities:
#         if entity.value == anchor:
#             anchor_value_count += 1
#     assert anchor_value_count == 1
#     for entity in solution.entities:
#         assert entity.anchor == anchor
