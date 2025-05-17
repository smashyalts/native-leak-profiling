import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.skullian.nativeleaks.Dependencies.versionCatalog

plugins {
    `java-library`
    id("com.gradleup.shadow")
}

group = rootProject.group
version = rootProject.version

repositories {
    mavenCentral()
    maven {
        name = "Sonatype Snapshots"
        url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencies {
    versionCatalog(project, "guava", ::implementation)

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
}

fun variables(): Map<String, String> = mapOf(
    "version" to rootProject.version.toString()
)

tasks {
    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.fork()
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }

    withType<ProcessResources> {
        val tokenMap = variables()
        filesMatching(listOf("**/*.json", "**/*.yml")) {
            expand(tokenMap)
        }
    }

    withType<ShadowJar> {
        archiveClassifier.set("")
        exclude(
            "**/*.kotlin_metadata",
            "**/*.kotlin_builtins",
            "META-INF/",
            "kotlin/**",
        )

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        archiveFileName.set("${rootProject.name}-${project.name}-v${rootProject.version}.jar")
        destinationDirectory.set(rootProject.rootDir.resolve("./libs"))
    }
}