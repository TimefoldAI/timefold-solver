# Changelog

{{changelogChanges}}
{{changelogContributors}}

# How to use Timefold Solver

To see _Timefold Solver_ in action, check out [the quickstarts](https://github.com/TimefoldAI/timefold-quickstarts).
_Timefold Solver_ jars are available in [the central maven repository](http://search.maven.org/#search|ga|1|ai.timefold.solver).


With Maven, just add the `timefold-solver-core` dependency in your `pom.xml` to get started:

    <dependency>
        <groupId>ai.timefold.solver</groupId>
        <artifactId>timefold-solver-core</artifactId>
        <version>0.9.39</version>
    </dependency>

Or better yet, import the `timefold-solver-bom` to avoid duplicating version numbers when adding other Timefold Solver dependencies later on:

    <project>
        ...
        <dependencyManagement>
            <dependencies>
                <dependency>
                    <groupId>ai.timefold.solver</groupId>
                    <artifactId>timefold-solver-bom</artifactId>
                    <type>pom</type>
                    <version>0.9.39</version>
                    <scope>import</scope>
                </dependency>
            </dependencies>
        </dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>ai.timefold.solver</groupId>
                <artifactId>timefold-solver-core</artifactId>
            </dependency>
            <dependency>
                 <groupId>ai.timefold.solver</groupId>
                 <artifactId>timefold-solver-test</artifactId>
                 <scope>test</scope>
             </dependency>
             ...
        </dependencies>
    </project>

With Gradle, just add the `timefold-solver-core` dependency in your `build.gradle` to get started:

    dependencies {
        implementation 'ai.timefold.solver:timefold-solver-core:0.9.39'
    }