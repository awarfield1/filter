plugins {
    `java-library`
}

description = "Filter model, matcher, string form, parser and structural visitors"
group = "com.example.filter"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Compile to a widely deployable baseline even though we build with a newer JDK.
    options.release.set(21)
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
}

// The library itself is intentionally dependency-free: it is pure Java and knows
// nothing about Spring, JPA, or any particular data store. Only the tests need
// anything on the classpath.
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}
