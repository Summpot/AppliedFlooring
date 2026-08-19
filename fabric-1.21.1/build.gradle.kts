plugins {
    id("com.gradleup.shadow")
}

val minecraftVersion = "1.21.1"
val architecturyVersion = "13.0.11"
val fabricLoaderVersion = "0.15.11"
val fabricApiVersion = "0.102.0+1.21.1"
val ae2Version = "16.0.2"

architectury {
    minecraft = minecraftVersion
    platformSetupLoomIde()
    fabric()
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
    named("developmentFabric") {
        extendsFrom(common)
    }
}

dependencies {
    "minecraft"("net.minecraft:minecraft:$minecraftVersion")
    "mappings"(project.extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>().officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.7+kotlin.2.2.21")
    modImplementation("dev.architectury:architectury-fabric:$architecturyVersion")

    modImplementation("appeng:appliedenergistics2-fabric:$ae2Version")

    common(project(path = ":common-1.21.1", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowBundle(project(path = ":common-1.21.1", configuration = "transformProductionFabric"))
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf(rootProject.file("fabric/src/main/java")))
        resources.setSrcDirs(listOf(rootProject.file("fabric/src/main/resources")))
    }
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.setSrcDirs(listOf(rootProject.file("fabric/src/main/kotlin")))
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    from(sourceSets.main.get().output)
    configurations = listOf(shadowBundle)
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
}
