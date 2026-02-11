@XmlAccessorType(XmlAccessType.FIELD)
@XmlJavaTypeAdapters({
        @XmlJavaTypeAdapter(value = PolymorphicScoreJaxbAdapter.class, type = Score.class),
        @XmlJavaTypeAdapter(value = JaxbOffsetDateTimeAdapter.class, type = OffsetDateTime.class)
})
package ai.timefold.solver.benchmark.impl.result;

import java.time.OffsetDateTime;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.io.jaxb.adapter.JaxbOffsetDateTimeAdapter;
import ai.timefold.solver.jaxb.api.score.PolymorphicScoreJaxbAdapter;
