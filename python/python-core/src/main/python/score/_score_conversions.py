from jpype import JConversion
from ._score import *


@JConversion('ai.timefold.solver.core.api.score.Score', exact=SimpleScore)
def _convert_simple_score(jcls, score: SimpleScore):
    return score._to_java_score()


@JConversion('ai.timefold.solver.core.api.score.Score', exact=HardSoftScore)
def _convert_hard_soft_score(jcls, score: HardSoftScore):
    return score._to_java_score()


@JConversion('ai.timefold.solver.core.api.score.Score', exact=HardMediumSoftScore)
def _convert_hard_medium_soft_score(jcls, score: HardMediumSoftScore):
    return score._to_java_score()


@JConversion('ai.timefold.solver.core.api.score.Score', exact=BendableScore)
def _convert_bendable_score(jcls, score: BendableScore):
    return score._to_java_score()


@JConversion('ai.timefold.solver.core.api.score.Score', exact=SimpleDecimalScore)
def _convert_simple_decimal_score(jcls, score: SimpleDecimalScore):
    return score._to_java_score()


@JConversion('ai.timefold.solver.core.api.score.Score', exact=HardSoftDecimalScore)
def _convert_hard_soft_decimal_score(jcls, score: HardSoftDecimalScore):
    return score._to_java_score()


@JConversion('ai.timefold.solver.core.api.score.Score', exact=HardMediumSoftDecimalScore)
def _convert_hard_medium_soft_decimal_score(jcls, score: HardMediumSoftDecimalScore):
    return score._to_java_score()


@JConversion('ai.timefold.solver.core.api.score.Score', exact=BendableDecimalScore)
def _convert_bendable_decimal_score(jcls, score: BendableDecimalScore):
    return score._to_java_score()
