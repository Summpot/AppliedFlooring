val minecraftVersion = "1.19.2"
val architecturyVersion = "6.6.92"
val fabricLoaderVersion = "0.18.0"
val ae2Version = "12.9.8"

architectury {
    minecraft = minecraftVersion
    common(listOf("fabric", "forge"))
}

dependencies {
    "minecraft"("net.minecraft:minecraft:$minecraftVersion")
    "mappings"(project.extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>().officialMojangMappings())

    modImplementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    modImplementation("dev.architectury:architectury:$architecturyVersion")
    modImplementation("appeng:appliedenergistics2-forge:$ae2Version:api")

    compileOnly("org.slf4j:slf4j-api:2.0.16")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

sourceSets {
    named("main") {
        java.setSrcDirs(
            listOf(
                rootProject.file("common/src/main/java"),
                project.file("src/versioned/java")
            )
        )
        resources.setSrcDirs(
            listOf(
                rootProject.file("common/src/main/resources")
            )
        )
    }
}

kotlin {
    sourceSets {
        named("main") {
            kotlin.setSrcDirs(
                listOf(
                    rootProject.file("common/src/main/kotlin"),
                    project.file("src/versioned/kotlin")
                )
            )
        }
    }
}
