from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

import pytest
from dataclasses import dataclass, field
from typing import Annotated, List, Optional


@planning_entity
@dataclass
class Queen:
    code: str
    column: int
    row: Annotated[Optional[int], PlanningVariable] = field(default=None)

    def get_column_index(self):
        return self.column

    def get_row_index(self):
        if self.row is None:
            return -1
        return self.row

    def get_ascending_diagonal_index(self):
        return self.get_column_index() + self.get_row_index()

    def get_descending_diagonal_index(self):
        return self.get_column_index() - self.get_row_index()

    def __eq__(self, other):
        return self.code == other.code

    def __hash__(self):
        return hash(self.code)


@planning_solution
@dataclass
class Solution:
    n: int
    queen_list: Annotated[List[Queen], PlanningEntityCollectionProperty]
    column_list: List[int]
    row_list: Annotated[List[int], ValueRangeProvider]
    score: Annotated[SimpleScore, PlanningScore] = field(default=None)


def test_constraint_match_disabled_incremental_score_calculator():
    class NQueensIncrementalScoreCalculator(IncrementalScoreCalculator):
        score: int
        row_index_map: dict
        ascending_diagonal_index_map: dict
        descending_diagonal_index_map: dict

        def reset_working_solution(self, working_solution: Solution):
            n = working_solution.n
            self.row_index_map = dict()
            self.ascending_diagonal_index_map = dict()
            self.descending_diagonal_index_map = dict()
            for i in range(n):
                self.row_index_map[i] = list()
                self.ascending_diagonal_index_map[i] = list()
                self.descending_diagonal_index_map[i] = list()
                if i != 0:
                    self.ascending_diagonal_index_map[n - 1 + i] = list()
                    self.descending_diagonal_index_map[-i] = list()
            self.score = 0
            for queen in working_solution.queen_list:
                self.insert(queen)

        def before_entity_added(self, entity: any):
            pass

        def after_entity_added(self, entity: any):
            self.insert(entity)

        def before_variable_changed(self, entity: any, variableName: str):
            self.retract(entity)

        def after_variable_changed(self, entity: any, variableName: str):
            self.insert(entity)

        def before_entity_removed(self, entity: any):
            self.retract(entity)

        def after_entity_removed(self, entity: any):
            pass

        def insert(self, queen: Queen):
            if queen.row is not None:
                row_index = queen.row
                row_index_list = self.row_index_map[row_index]
                self.score -= len(row_index_list)
                row_index_list.append(queen)
                ascending_diagonal_index_list = self.ascending_diagonal_index_map[queen.get_ascending_diagonal_index()]
                self.score -= len(ascending_diagonal_index_list)
                ascending_diagonal_index_list.append(queen)
                descending_diagonal_index_list = self.descending_diagonal_index_map[queen.get_descending_diagonal_index()]
                self.score -= len(descending_diagonal_index_list)
                descending_diagonal_index_list.append(queen)

        def retract(self, queen: Queen):
            if queen.row is not None:
                row_index = queen.row
                row_index_list = self.row_index_map[row_index]
                row_index_list.remove(queen)
                self.score += len(row_index_list)
                ascending_diagonal_index_list = self.ascending_diagonal_index_map[queen.get_ascending_diagonal_index()]
                ascending_diagonal_index_list.remove(queen)
                self.score += len(ascending_diagonal_index_list)
                descending_diagonal_index_list = self.descending_diagonal_index_map[queen.get_descending_diagonal_index()]
                descending_diagonal_index_list.remove(queen)
                self.score += len(descending_diagonal_index_list)

        def calculate_score(self) -> SimpleScore:
            return SimpleScore.of(self.score)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Queen],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            incremental_score_calculator_class=NQueensIncrementalScoreCalculator
        ),
        termination_config=TerminationConfig(
            best_score_limit='0'
        )
    )
    problem: Solution = Solution(4,
                                 [Queen('A', 0), Queen('B', 1), Queen('C', 2), Queen('D', 3)],
                                 [0, 1, 2, 3],
                                 [0, 1, 2, 3])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 0
    for i in range(4):
        for j in range(i + 1, 4):
            left_queen = solution.queen_list[i]
            right_queen = solution.queen_list[j]
            assert left_queen.row is not None and right_queen.row is not None
            assert left_queen.row != right_queen.row
            assert left_queen.get_ascending_diagonal_index() != right_queen.get_ascending_diagonal_index()
            assert left_queen.get_descending_diagonal_index() != right_queen.get_descending_diagonal_index()


@pytest.mark.skip(reason="Special case where you want to convert all items of the list before returning."
                         "Doing this for all conversions would be expensive."
                         "This feature is not that important, so skipping for now.")
def test_constraint_match_enabled_incremental_score_calculator():
    @incremental_score_calculator
    class NQueensIncrementalScoreCalculator(ConstraintMatchAwareIncrementalScoreCalculator):
        score: int
        row_index_map: dict
        ascending_diagonal_index_map: dict
        descending_diagonal_index_map: dict

        def reset_working_solution(self, working_solution: Solution, constraint_match_enabled=False):
            n = working_solution.n
            self.row_index_map = dict()
            self.ascending_diagonal_index_map = dict()
            self.descending_diagonal_index_map = dict()
            for i in range(n):
                self.row_index_map[i] = list()
                self.ascending_diagonal_index_map[i] = list()
                self.descending_diagonal_index_map[i] = list()
                if i != 0:
                    self.ascending_diagonal_index_map[n - 1 + i] = list()
                    self.descending_diagonal_index_map[-i] = list()
            self.score = 0
            for queen in working_solution.queen_list:
                self.insert(queen)

        def before_entity_added(self, entity: any):
            pass

        def after_entity_added(self, entity: any):
            self.insert(entity)

        def before_variable_changed(self, entity: any, variableName: str):
            self.retract(entity)

        def after_variable_changed(self, entity: any, variableName: str):
            self.insert(entity)

        def before_entity_removed(self, entity: any):
            self.retract(entity)

        def after_entity_removed(self, entity: any):
            pass

        def insert(self, queen: Queen):
            row = queen.row
            if row is not None:
                row_index = queen.row
                row_index_list = self.row_index_map[row_index]
                self.score -= len(row_index_list)
                row_index_list.append(queen)
                ascending_diagonal_index_list = self.ascending_diagonal_index_map[queen.get_ascending_diagonal_index()]
                self.score -= len(ascending_diagonal_index_list)
                ascending_diagonal_index_list.append(queen)
                descending_diagonal_index_list = self.descending_diagonal_index_map[queen.get_descending_diagonal_index()]
                self.score -= len(descending_diagonal_index_list)
                descending_diagonal_index_list.append(queen)

        def retract(self, queen: Queen):
            row = queen.row
            if row is not None:
                row_index = queen.row
                row_index_list = self.row_index_map[row_index]
                row_index_list.remove(queen)
                self.score += len(row_index_list)
                ascending_diagonal_index_list = self.ascending_diagonal_index_map[queen.get_ascending_diagonal_index()]
                ascending_diagonal_index_list.remove(queen)
                self.score += len(ascending_diagonal_index_list)
                descending_diagonal_index_list = self.descending_diagonal_index_map[queen.get_descending_diagonal_index()]
                descending_diagonal_index_list.remove(queen)
                self.score += len(descending_diagonal_index_list)

        def calculate_score(self) -> SimpleScore:
            return SimpleScore.of(self.score)

        def get_constraint_match_totals(self):
            row_conflict_constraint_match_total = DefaultConstraintMatchTotal(
                'NQueens',
                'Row Conflict',
                SimpleScore.ONE)
            ascending_diagonal_constraint_match_total = DefaultConstraintMatchTotal(
                'NQueens',
                'Ascending Diagonal Conflict',
                SimpleScore.ONE)
            descending_diagonal_constraint_match_total = DefaultConstraintMatchTotal(
                'NQueens',
                'Descending Diagonal Conflict',
                SimpleScore.ONE)
            for row, queens in self.row_index_map.items():
                if len(queens) > 1:
                    row_conflict_constraint_match_total.addConstraintMatch(queens,
                                                                           SimpleScore.of(
                                                                               -len(queens) + 1))
            for row, queens in self.ascending_diagonal_index_map.items():
                if len(queens) > 1:
                    ascending_diagonal_constraint_match_total.addConstraintMatch(queens,
                                                                                 SimpleScore.of(
                                                                                     -len(queens) + 1))
            for row, queens in self.descending_diagonal_index_map.items():
                if len(queens) > 1:
                    descending_diagonal_constraint_match_total.addConstraintMatch(queens,
                                                                                  SimpleScore.of(
                                                                                      -len(queens) + 1))
            return [
                row_conflict_constraint_match_total,
                ascending_diagonal_constraint_match_total,
                descending_diagonal_constraint_match_total
            ]

        def get_indictment_map(self):
            return None

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Queen],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            incremental_score_calculator_class=NQueensIncrementalScoreCalculator
        ),
        termination_config=TerminationConfig(
            best_score_limit='0'
        )
    )
    problem: Solution = Solution(4,
                                 [Queen('A', 0), Queen('B', 1), Queen('C', 2), Queen('D', 3)],
                                 [0, 1, 2, 3],
                                 [0, 1, 2, 3])
    solver_factory = SolverFactory.create(solver_config)
    solver = solver_factory.build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 0
    for i in range(4):
        for j in range(i + 1, 4):
            left_queen = solution.queen_list[i]
            right_queen = solution.queen_list[j]
            assert left_queen.row is not None and right_queen.row is not None
            assert left_queen.row != right_queen.row
            assert left_queen.get_ascending_diagonal_index() != right_queen.get_ascending_diagonal_index()
            assert left_queen.get_descending_diagonal_index() != right_queen.get_descending_diagonal_index()

    score_manager = SolutionManager.create(solver_factory)
    constraint_match_total_map = score_manager.explain(solution).constraint_match_total_map
    row_conflict = constraint_match_total_map.get('NQueens/Row Conflict')
    ascending_diagonal_conflict = constraint_match_total_map.get('NQueens/Ascending Diagonal Conflict')
    descending_diagonal_conflict = constraint_match_total_map.get('NQueens/Descending Diagonal Conflict')
    assert row_conflict.score.score == 0
    assert ascending_diagonal_conflict.score.score == 0
    assert descending_diagonal_conflict.score.score == 0

    bad_solution = Solution(4,
                            [Queen('A', 0, 0), Queen('B', 1, 1), Queen('C', 2, 0), Queen('D', 3, 1)],
                            [0, 1, 2, 3],
                            [0, 1, 2, 3])
    score_explanation = score_manager.explain(bad_solution)
    assert score_explanation.score.score == -5
    constraint_match_total_map = score_explanation.constraint_match_total_map
    row_conflict = constraint_match_total_map.get('NQueens/Row Conflict')
    ascending_diagonal_conflict = constraint_match_total_map.get('NQueens/Ascending Diagonal Conflict')
    descending_diagonal_conflict = constraint_match_total_map.get('NQueens/Descending Diagonal Conflict')
    assert row_conflict.score.score == -2  # (A, C), (B, D)
    assert ascending_diagonal_conflict.score.score == -1  # (B, C)
    assert descending_diagonal_conflict.score.score == -2  # (A, B), (C, D)
    indictment_map = score_explanation.indictment_map
    assert indictment_map.get(bad_solution.queen_list[0]).constraint_match_count == 2
    assert indictment_map.get(bad_solution.queen_list[1]).constraint_match_count == 3
    assert indictment_map.get(bad_solution.queen_list[2]).constraint_match_count == 3
    assert indictment_map.get(bad_solution.queen_list[3]).constraint_match_count == 2


def test_error_message_for_missing_methods():
    with pytest.raises(TypeError):  # Exact error message from ABC changes between versions
        class IncrementalScoreCalculatorMissingMethods(IncrementalScoreCalculator):
            def before_entity_added(self, entity):
                pass

            def after_entity_added(self, entity):
                pass

            def before_variable_changed(self, entity, variable_name: str):
                pass

            def after_variable_changed(self, entity, variable_name: str):
                pass

        score_calculator = IncrementalScoreCalculatorMissingMethods()
