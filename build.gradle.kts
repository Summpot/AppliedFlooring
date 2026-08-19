import org.gradle.jvm.JvmLibrary
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.language.base.artifact.SourcesArtifact
import org.gradle.api.file.DuplicatesStrategy
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

plugins {
    id("dev.architectury.loom") version "1.17-SNAPSHOT" apply false
    id("architectury-plugin") version "3.5-SNAPSHOT"
    id("com.gradleup.shadow") version "9.6.1" apply false
    kotlin("jvm") version "2.2.21" apply false
}

fun stripOrphanMultiReleaseAttribute(jarFile: java.io.File) {
    try {
        FileSystems.newFileSystem(URI.create("jar:${jarFile.toURI()}"), emptyMap<String, Any>()).use { fs ->
            val versions = fs.getPath("META-INF/versions")
            if (versions.exists() && versions.isDirectory()) return

            val manifest = fs.getPath("META-INF/MANIFEST.MF")
            if (!manifest.exists()) return

            val lines = manifest.readLines()
            if (lines.none { it.startsWith("Multi-Release:", ignoreCase = true) }) return

            manifest.writeLines(lines.filterNot { it.startsWith("Multi-Release:", ignoreCase = true) })
        }
    } catch (_: Exception) {}
}

abstract class ArchitecturyTransformMutex : BuildService<BuildServiceParameters.None>

val architecturyTransformMutex = gradle.sharedServices.registerIfAbsent(
    "architecturyTransformMutex",
    ArchitecturyTransformMutex::class.java,
) {
    maxParallelUsages.set(1)
}

allprojects {
    group = project.property("maven_group").toString()
    version = project.property("mod_version").toString()
}

gradle.projectsEvaluated {
    val transformTasks = subprojects
        .flatMap { subproject ->
            subproject.tasks.names
                .filter { it.startsWith("transformProduction") }
                .map { taskName ->
                    "${subproject.path}:$taskName" to subproject.tasks.named(taskName)
                }
        }
        .sortedBy { it.first }
        .map { it.second }

    for (index in 1 until transformTasks.size) {
        transformTasks[index].configure {
            mustRunAfter(transformTasks[index - 1])
        }
    }
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.configureEach {
        if (name.startsWith("transformProduction")) {
            usesService(architecturyTransformMutex)
        }
    }

    extensions.configure<BasePluginExtension> {
        archivesName.set("${rootProject.property("archives_name")}-${project.name}")
    }

    repositories {
        mavenCentral()
        maven {
            name = "NeoForged"
            url = uri("https://maven.neoforged.net/releases")
        }
        maven {
            name = "ModMaven"
            url = uri("https://modmaven.dev/")
        }
        maven {
            name = "Fuzs Mod Resources"
            url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
        }
    }

    configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
        silentMojangMappingsLicense()
    }

    configure<JavaPluginExtension> {
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.release.set(21)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    pluginManager.withPlugin("com.gradleup.shadow") {
        tasks.withType<ShadowJar>().configureEach {
            exclude("META-INF/versions/**")
            exclude("module-info.class")
            exclude("META-INF/**/module-info.class")

            doLast {
                stripOrphanMultiReleaseAttribute(archiveFile.get().asFile)
            }
        }
    }

    project.file(".gradle/.architectury-transformer").mkdirs()
}
