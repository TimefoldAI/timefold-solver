package ai.timefold.solver.core.config.constructionheuristic.placer;

import jakarta.xml.bind.annotation.XmlSeeAlso;

import ai.timefold.solver.core.config.AbstractConfig;

/**
 * General superclass for {@link QueuedEntityPlacerConfig} and {@link PooledEntityPlacerConfig}.
 */

@XmlSeeAlso({
        PooledEntityPlacerConfig.class,
        QueuedEntityPlacerConfig.class,
        QueuedValuePlacerConfig.class
})
public abstract class EntityPlacerConfig<Config_ extends EntityPlacerConfig<Config_>> extends AbstractConfig<Config_> {

}
