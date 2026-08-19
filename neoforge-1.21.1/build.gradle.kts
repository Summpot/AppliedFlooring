plugins {
    id("com.gradleup.shadow")
}

val minecraftVersion = "1.21.1"
val neoforgeVersion = "21.1.187"
val architecturyVersion = "13.0.11"
val ae2Version = "19.0.4-alpha"

architectury {
    minecraft = minecraftVersion
    platformSetupLoomIde()
    neoForge()
}

configure<net.fabricmc.loom.api.LoomGradleExtensionAPI> {
    neoForge {
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
    named("developmentNeoForge") {
        extendsFrom(common)
    }
}

dependencies {
    "minecraft"("net.minecraft:minecraft:$minecraftVersion")
    "mappings"(project.extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>().officialMojangMappings())

    "neoForge"("net.neoforged:neoforge:$neoforgeVersion")
    "forgeRuntimeLibrary"("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
    "forgeRuntimeLibrary"("org.jetbrains.kotlin:kotlin-reflect:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")
    implementation("org.jetbrains.kotlin:kotlin-reflect:2.2.21")
    modImplementation("dev.architectury:architectury-neoforge:$architecturyVersion")

    modImplementation("appeng:appliedenergistics2-neoforge:$ae2Version")

    common(project(path = ":common-1.21.1", configuration = "namedElements")) {
        isTransitive = false
    }
    shadowBundle(project(path = ":common-1.21.1", configuration = "transformProductionNeoForge"))
}

sourceSets {
    named("main") {
        java.setSrcDirs(listOf(rootProject.file("neoforge/src/main/java")))
        resources.setSrcDirs(listOf(rootProject.file("neoforge/src/main/resources")))
    }
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.setSrcDirs(listOf(rootProject.file("neoforge/src/main/kotlin")))
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("META-INF/neoforge.mods.toml") {
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
