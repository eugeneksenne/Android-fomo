# Gradle Wrapper

`gradle-wrapper.jar` is intentionally **not** present in this directory yet and
must be generated once, from a machine with network access, before CI or a
fresh clone can build:

```bash
# From the repository root, using a locally installed Gradle 9.3.1+:
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
```

This produces `gradle/wrapper/gradle-wrapper.jar` (~44 KB). Commit it — the
wrapper JAR is meant to be checked in, and is what makes builds reproducible
across machines and CI.

Verify the distribution matches AGP's requirement: **AGP 9.1.x requires Gradle
9.3.1 or newer and JDK 17.**
