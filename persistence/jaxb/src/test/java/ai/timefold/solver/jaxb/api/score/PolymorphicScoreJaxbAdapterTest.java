package ai.timefold.solver.jaxb.api.score;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringReader;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.io.jaxb.GenericJaxbIO;

import org.junit.jupiter.api.Test;

class PolymorphicScoreJaxbAdapterTest {

    private final PolymorphicScoreJaxbAdapter scoreJaxbAdapter = new PolymorphicScoreJaxbAdapter();

    @Test
    void marshall() {
        Score<?> score = SimpleScore.of(1);
        PolymorphicScoreJaxbAdapter.JaxbAdaptedScore adaptedScore = scoreJaxbAdapter.marshal(score);
        assertThat(adaptedScore.getScoreClassName()).isEqualTo(SimpleScore.class.getName());
        assertThat(adaptedScore.getScoreString()).isEqualTo(score.toString());
    }

    @Test
    void unmarshall() {
        String xmlString = "<dummy>"
                + "<score class=\"ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore\">-1hard/-10soft</score>"
                + "</dummy>";

        GenericJaxbIO<DummyRootElement> xmlIO = new GenericJaxbIO<>(DummyRootElement.class);
        DummyRootElement dummyRootElement = xmlIO.read(new StringReader(xmlString));

        assertThat(dummyRootElement.score).isEqualTo(HardSoftLongScore.of(-1L, -10L));
    }

    @XmlRootElement(name = "dummy")
    private static class DummyRootElement {

        @XmlJavaTypeAdapter(PolymorphicScoreJaxbAdapter.class)
        private Score<?> score;

        private DummyRootElement() {
            // Required by JAXB
        }

        private DummyRootElement(Score<?> score) {
            this.score = score;
        }
    }
}
