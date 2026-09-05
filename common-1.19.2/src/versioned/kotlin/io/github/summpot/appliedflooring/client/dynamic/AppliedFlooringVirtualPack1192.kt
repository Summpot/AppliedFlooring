package io.github.summpot.appliedflooring.client.dynamic

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionSerializer
import net.minecraft.server.packs.metadata.pack.PackMetadataSection
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.function.Predicate

class AppliedFlooringVirtualPack1192 : PackResources {

    override fun getRootResource(name: String): InputStream {
        throw FileNotFoundException("Root resource $name not found in virtual pack")
    }

    override fun getResource(type: PackType, location: ResourceLocation): InputStream {
        if (type != PackType.CLIENT_RESOURCES || location.namespace != "appliedflooring") {
            throw FileNotFoundException(location.toString())
        }
        VirtualTextureSynthesizer.ensureInitialized()
        val bytes = VirtualTextureSynthesizer.RESOURCES[location.path]
            ?: throw FileNotFoundException(location.toString())
        return ByteArrayInputStream(bytes)
    }

    override fun getResources(
        type: PackType,
        namespace: String,
        path: String,
        filter: Predicate<ResourceLocation>
    ): Collection<ResourceLocation> {
        if (type != PackType.CLIENT_RESOURCES || namespace != "appliedflooring") return emptyList()
        VirtualTextureSynthesizer.ensureInitialized()
        val result = mutableListOf<ResourceLocation>()
        for (relPath in VirtualTextureSynthesizer.RESOURCE_PATHS) {
            if (relPath.startsWith(path)) {
                val loc = ResourceLocation("appliedflooring", relPath)
                if (filter.test(loc)) {
                    result.add(loc)
                }
            }
        }
        return result
    }

    override fun hasResource(type: PackType, location: ResourceLocation): Boolean {
        if (type != PackType.CLIENT_RESOURCES || location.namespace != "appliedflooring") return false
        VirtualTextureSynthesizer.ensureInitialized()
        return VirtualTextureSynthesizer.RESOURCES.containsKey(location.path)
    }

    override fun getNamespaces(type: PackType): Set<String> {
        return if (type == PackType.CLIENT_RESOURCES) setOf("appliedflooring") else emptySet()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getMetadataSection(serializer: MetadataSectionSerializer<T>): T? {
        if (serializer.metadataSectionName == "pack") {
            return PackMetadataSection(Component.literal("Applied Flooring Dynamic Textures"), 9) as? T
        }
        return null
    }

    override fun getName(): String = "appliedflooring:virtual_textures"

    override fun close() {}
}
