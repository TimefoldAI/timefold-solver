package ai.timefold.solver.migration.jakarta;

import org.openrewrite.ExecutionContext;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;

final class InternalTimefoldMavenVisitor extends MavenVisitor<ExecutionContext> {

    private static final String GROUP_ID = "ai.timefold.solver";
    private static final String ARTIFACT_ID_PREFIX = "timefold-solver-";

    @Override
    public Xml visitDocument(Xml.Document document, ExecutionContext executionContext) {
        ResolvedPom pom = getResolutionResult().getPom();
        if (pom.getGroupId().equals(GROUP_ID) && pom.getArtifactId().startsWith(ARTIFACT_ID_PREFIX)) {
            return SearchResult.found(document);
        }
        return document;
    }
}
