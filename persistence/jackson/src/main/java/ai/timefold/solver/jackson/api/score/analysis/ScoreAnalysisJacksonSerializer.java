package ai.timefold.solver.jackson.api.score.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class ScoreAnalysisJacksonSerializer<Score_ extends Score<Score_>> extends JsonSerializer<ScoreAnalysis<Score_>> {
    @Override
    public void serialize(ScoreAnalysis<Score_> value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("score", value.score().toString());

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
            result.add(constraintAnalysisMap);
        });
        gen.writeObjectField("constraints", result);
        gen.writeEndObject();
    }

}
