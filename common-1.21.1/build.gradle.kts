val minecraftVersion = "1.21.1"
val architecturyVersion = "13.0.11"
val ae2Version = "16.0.2"

architectury {
    minecraft = minecraftVersion
    common(listOf("neoforge"))
}

dependencies {
    "minecraft"("net.minecraft:minecraft:$minecraftVersion")
    "mappings"(project.extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>().officialMojangMappings())

    modImplementation("dev.architectury:architectury:$architecturyVersion")
    modImplementation("appeng:appliedenergistics2-neoforge:$ae2Version:api")

    compileOnly("org.slf4j:slf4j-api:2.0.16")
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
