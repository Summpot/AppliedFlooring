package io.github.summpot.appliedflooring.util

import net.minecraft.world.item.ItemStack

object FlooringWrenchHelper {
    fun isWrench(stack: ItemStack): Boolean {
        if (stack.isEmpty) return false
        val desc = stack.descriptionId.lowercase()
        if (desc.contains("wrench") || desc.contains("network_tool")) return true
        val tags = stack.tags.toList()
        return tags.any { it.location.toString().lowercase().contains("wrench") }
    }
}
