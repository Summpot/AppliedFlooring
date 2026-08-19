plugins {
    id("com.gradleup.shadow")
}

val minecraftVersion = "1.20.1"
val forgeVersion = "1.20.1-47.4.10"
val architecturyVersion = "9.2.14"
val ae2Version = "15.2.13"

architectury {
    minecraft = minecraftVersion
    platformSetupLoomIde()
    forge()
}

configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
    forge {
    }
}

val common: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

val shadowBundle: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

configurations {
    compileClasspath.get().extendsFrom(common)
    runtimeClasspath.get().extendsFrom(common)
    named("developmentForge") {
        extendsFrom(common)
    }
}

repositories {
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

dependencies {
    "minecraft"("net.minecraft:minecraft:$minecraftVersion")
    "mappings"(project.extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>().officialMojangMappings())

    "forge"("net.minecraftforge:forge:$forgeVersion")
    implementation("thedarkcolour:kotlinforforge:4.10.0")
    modImplementation("dev.architectury:architectury-forge:$architecturyVersion")

    modImplementation("appeng:appliedenergistics2-forge:$ae2Version")

    common(project(path = ":common-1.20.1", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowBundle(project(path = ":common-1.20.1", configuration = "transformProductionForge"))
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf(rootProject.file("forge/src/main/java")))
        resources.setSrcDirs(listOf(rootProject.file("forge/src/main/resources")))
    }
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.setSrcDirs(listOf(rootProject.file("forge/src/main/kotlin")))
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/mods.toml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
}
