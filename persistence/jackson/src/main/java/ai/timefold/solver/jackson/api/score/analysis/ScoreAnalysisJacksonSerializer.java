package ai.timefold.solver.jackson.api.score.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class ScoreAnalysisJacksonSerializer<Score_ extends Score<Score_>> extends ValueSerializer<ScoreAnalysis<Score_>> {
    @Override
    public void serialize(ScoreAnalysis<Score_> value, JsonGenerator gen, SerializationContext serializers)
            throws JacksonException {
        gen.writeStartObject();
        gen.writeStringProperty("score", value.score().toString());
        gen.writeBooleanProperty("initialized", value.isSolutionInitialized());

        List<Map<String, Object>> result = new ArrayList<>();
        value.constraintMap().forEach((constraintRef, constraintAnalysis) -> {
            Map<String, Object> constraintAnalysisMap = new LinkedHashMap<>();
            constraintAnalysisMap.put("package", constraintRef.packageName());
            constraintAnalysisMap.put("name", constraintRef.constraintName());
            constraintAnalysisMap.put("weight", constraintAnalysis.weight().toString());
            constraintAnalysisMap.put("score", constraintAnalysis.score().toString());
            if (constraintAnalysis.matches() != null) {
                List<Map<String, Object>> matchAnalysis = new ArrayList<>(constraintAnalysis.matches().size());
                constraintAnalysis.matches().forEach(match -> {
                    Map<String, Object> matchMap = new LinkedHashMap<>();
                    matchMap.put("score", match.score().toString());
                    if (match.justification() instanceof DefaultConstraintJustification justification) {
                        matchMap.put("justification", justification.getFacts());
                    } else {
                        matchMap.put("justification", match.justification());
                    }
                    matchAnalysis.add(matchMap);
                });
                constraintAnalysisMap.put("matches", matchAnalysis);
            }
            if (constraintAnalysis.matchCount() != -1) {
                constraintAnalysisMap.put("matchCount", constraintAnalysis.matchCount());
            }
            result.add(constraintAnalysisMap);
        });
        gen.writePOJOProperty("constraints", result);
        gen.writeEndObject();
    }

}
