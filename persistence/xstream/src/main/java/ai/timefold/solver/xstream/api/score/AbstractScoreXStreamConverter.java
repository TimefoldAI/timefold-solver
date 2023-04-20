package ai.timefold.solver.xstream.api.score;

import ai.timefold.solver.xstream.api.score.buildin.bendable.BendableScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.bendablebigdecimal.BendableBigDecimalScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.bendablelong.BendableLongScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.hardmediumsoft.HardMediumSoftScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.hardsoft.HardSoftScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.hardsoftlong.HardSoftLongScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.simple.SimpleScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.simplebigdecimal.SimpleBigDecimalScoreXStreamConverter;
import ai.timefold.solver.xstream.api.score.buildin.simplelong.SimpleLongScoreXStreamConverter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;

/**
 * @deprecated Prefer JAXB for serialization into XML.
 */
@Deprecated(forRemoval = true)
public abstract class AbstractScoreXStreamConverter implements Converter {

    public static void registerScoreConverters(XStream xStream) {
        xStream.registerConverter(new SimpleScoreXStreamConverter());
        xStream.registerConverter(new SimpleLongScoreXStreamConverter());
        xStream.registerConverter(new SimpleBigDecimalScoreXStreamConverter());

        xStream.registerConverter(new HardSoftScoreXStreamConverter());
        xStream.registerConverter(new HardSoftLongScoreXStreamConverter());
        xStream.registerConverter(new HardSoftBigDecimalScoreXStreamConverter());

        xStream.registerConverter(new HardMediumSoftScoreXStreamConverter());
        xStream.registerConverter(new HardMediumSoftLongScoreXStreamConverter());
        xStream.registerConverter(new HardMediumSoftBigDecimalScoreXStreamConverter());

        xStream.registerConverter(new BendableScoreXStreamConverter());
        xStream.registerConverter(new BendableLongScoreXStreamConverter());
        xStream.registerConverter(new BendableBigDecimalScoreXStreamConverter());
    }

}
