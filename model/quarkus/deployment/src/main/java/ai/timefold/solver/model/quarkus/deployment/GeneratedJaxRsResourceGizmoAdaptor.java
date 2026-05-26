package ai.timefold.solver.model.quarkus.deployment;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.Indexer;

import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.bootstrap.BootstrapDebug;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.resteasy.reactive.spi.AdditionalResourceClassBuildItem;

public class GeneratedJaxRsResourceGizmoAdaptor implements ClassOutput {

    private final BuildProducer<AdditionalResourceClassBuildItem> classOutput;
    private final BuildProducer<GeneratedBeanBuildItem> generatedClasses;
    private final Map<String, StringWriter> sources;
    private final String path;
    private final CombinedIndexBuildItem combinedIndex;
    private String generatedName;

    public GeneratedJaxRsResourceGizmoAdaptor(CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalResourceClassBuildItem> classOutput, BuildProducer<GeneratedBeanBuildItem> generatedClasses,
            String path) {
        this.classOutput = classOutput;
        this.generatedClasses = generatedClasses;
        this.sources = BootstrapDebug.DEBUG_SOURCES_DIR != null ? new ConcurrentHashMap<>() : null;
        this.path = path;
        this.combinedIndex = combinedIndex;
    }

    @Override
    public void write(String className, byte[] bytes) {
        String source = null;
        if (sources != null) {
            StringWriter sw = sources.get(className);
            if (sw != null) {
                source = sw.toString();
            }
        }
        generatedClasses.produce(new GeneratedBeanBuildItem(className, bytes, source));
        Indexer additionalBeanIndexer = new Indexer();
        CompositeIndex newIndex = CompositeIndex.create(combinedIndex.getIndex());

        IndexingUtil.indexClass(generatedName, additionalBeanIndexer, newIndex, new HashSet<>(),
                Thread.currentThread().getContextClassLoader(), bytes);

        ClassInfo indexed = additionalBeanIndexer.complete().getClassByName(generatedName);
        classOutput.produce(new AdditionalResourceClassBuildItem(indexed, path));
    }

    @Override
    public Writer getSourceWriter(String className) {
        if (sources != null) {
            StringWriter writer = new StringWriter();
            sources.put(className, writer);
            return writer;
        }
        return ClassOutput.super.getSourceWriter(className);
    }

    public void setGeneratedName(String generatedName) {
        this.generatedName = generatedName;
    }

}