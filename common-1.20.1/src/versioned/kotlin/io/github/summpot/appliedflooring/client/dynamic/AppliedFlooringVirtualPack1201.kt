package io.github.summpot.appliedflooring.client.dynamic

import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackResources
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.metadata.MetadataSectionSerializer
import net.minecraft.server.packs.metadata.pack.PackMetadataSection
import net.minecraft.server.packs.resources.IoSupplier
import java.io.ByteArrayInputStream
import java.io.InputStream

class AppliedFlooringVirtualPack1201 : PackResources {

    override fun getRootResource(vararg paths: String): IoSupplier<InputStream>? = null

    override fun getResource(type: PackType, location: ResourceLocation): IoSupplier<InputStream>? {
        if (type != PackType.CLIENT_RESOURCES || location.namespace != "appliedflooring") return null
        VirtualTextureSynthesizer.ensureInitialized()
        val bytes = VirtualTextureSynthesizer.RESOURCES[location.path] ?: return null
        return IoSupplier { ByteArrayInputStream(bytes) }
    }

    override fun listResources(type: PackType, namespace: String, path: String, output: PackResources.ResourceOutput) {
        if (type != PackType.CLIENT_RESOURCES || namespace != "appliedflooring") return
        VirtualTextureSynthesizer.ensureInitialized()
        for ((relPath, bytes) in VirtualTextureSynthesizer.RESOURCES) {
            if (relPath.startsWith(path)) {
                val loc = ResourceLocation("appliedflooring", relPath)
                output.accept(loc, IoSupplier { ByteArrayInputStream(bytes) })
            }
        }
    }

    override fun getNamespaces(type: PackType): Set<String> {
        return if (type == PackType.CLIENT_RESOURCES) setOf("appliedflooring") else emptySet()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getMetadataSection(serializer: MetadataSectionSerializer<T>): T? {
        if (serializer.metadataSectionName == "pack") {
            return PackMetadataSection(Component.literal("Applied Flooring Dynamic Textures"), 15) as? T
        }
        return null
    }

    override fun packId(): String = "appliedflooring:virtual_textures"

    override fun isBuiltin(): Boolean = true

    override fun close() {}
}
