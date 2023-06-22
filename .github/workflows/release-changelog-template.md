# Changelog

{{changelogChanges}}
{{changelogContributors}}

# How to use Timefold Solver

To see _Timefold Solver_ in action, check out [the quickstarts](https://github.com/TimefoldAI/timefold-quickstarts).

With Maven, just add the `timefold-solver-core` dependency in your `pom.xml` to get started:

[//]: # (Using &nbsp; because due to a JReleaser bug any other whitespace is going to be lost.)
```xml
<dependency>
&nbsp;&nbsp;<groupId>ai.timefold.solver</groupId>
&nbsp;&nbsp;<artifactId>timefold-solver-core</artifactId>
&nbsp;&nbsp;<version>{{versionName}}</version>
</dependency>
```

Or better yet, import the `timefold-solver-bom` to avoid duplicating version numbers when adding other Timefold Solver dependencies later on:

```xml
<project>
&nbsp;&nbsp;...
&nbsp;&nbsp;<dependencyManagement>
&nbsp;&nbsp;&nbsp;&nbsp;<dependencies>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<dependency>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<groupId>ai.timefold.solver</groupId>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<artifactId>timefold-solver-bom</artifactId>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<type>pom</type>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<version>{{versionName}}</version>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<scope>import</scope>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</dependency>
&nbsp;&nbsp;&nbsp;&nbsp;</dependencies>
&nbsp;&nbsp;</dependencyManagement>
&nbsp;&nbsp;<dependencies>
&nbsp;&nbsp;&nbsp;&nbsp;<dependency>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<groupId>ai.timefold.solver</groupId>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<artifactId>timefold-solver-core</artifactId>
&nbsp;&nbsp;&nbsp;&nbsp;</dependency>
&nbsp;&nbsp;&nbsp;&nbsp;<dependency>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<groupId>ai.timefold.solver</groupId>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<artifactId>timefold-solver-test</artifactId>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<scope>test</scope>
&nbsp;&nbsp;&nbsp;&nbsp;</dependency>
&nbsp;&nbsp;&nbsp;&nbsp;...
&nbsp;&nbsp;</dependencies>
</project>
```

With Gradle, just add the `timefold-solver-core` dependency in your `build.gradle` to get started:

```
dependencies {
&nbsp;&nbsp;implementation 'ai.timefold.solver:timefold-solver-core:{{versionName}}'
}
```