val minecraftVersion = "1.20.1"
val architecturyVersion = "9.2.14"
val fabricLoaderVersion = "0.18.0"
val ae2Version = "15.2.13"

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
