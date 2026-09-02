package io.github.summpot.appliedflooring.blockentity

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

enum class FloorShape(val id: Int, val displayNameZh: String, val displayNameEn: String) {
    SQUARE(0, "方形", "Square"),
    CIRCLE(1, "圆形", "Circle"),
    HOLLOW_FRAME(2, "边框", "Hollow Frame"),
    CHECKERBOARD(3, "棋盘", "Checkerboard");

    val translationKey: String get() = "gui.appliedflooring.shape." + name.lowercase()
    fun getDisplayName(): Component = Component.translatable(translationKey)

    companion object {
        fun byId(id: Int): FloorShape = values().firstOrNull { it.id == id } ?: SQUARE
    }
}

enum class BuilderStatus(val id: Int, val descriptionZh: String, val descriptionEn: String) {
    IDLE(0, "就绪", "Ready"),
    BUILDING(1, "铺设中", "Building"),
    PAUSED(2, "已暂停", "Paused"),
    DONE(3, "已完成", "Completed"),
    NO_POWER(4, "缺少AE能源", "Out of Power"),
    NO_ITEMS(5, "缺少地板材料", "Out of Flooring"),
    BLOCKED(6, "目标受阻", "Blocked");

    val translationKey: String get() = "gui.appliedflooring.status." + name.lowercase()
    fun getDisplayName(): Component = Component.translatable(translationKey)

    companion object {
        fun byId(id: Int): BuilderStatus = values().firstOrNull { it.id == id } ?: IDLE
    }
}

enum class ReplaceMode(val id: Int, val displayNameZh: String, val displayNameEn: String) {
    AIR_ONLY(0, "安全 (仅空气/流体)", "Safe (Air/Fluid Only)"),
    REPLACE_ALL(1, "覆盖全部方块", "Overwrite All");

    val buttonTranslationKey: String get() = "gui.appliedflooring.replace_mode." + name.lowercase()
    val descTranslationKey: String get() = "gui.appliedflooring.replace_mode." + name.lowercase() + ".desc"
    fun getButtonText(): Component = Component.translatable(buttonTranslationKey)
    fun getTooltipText(): Component = Component.translatable(descTranslationKey)

    companion object {
        fun byId(id: Int): ReplaceMode = values().firstOrNull { it.id == id } ?: AIR_ONLY
    }
}

object LaserFloorPlan {

    /**
     * Generates a list of relative (dx, dz) block coordinates to be placed,
     * sorted in an outward spiral from the center (0, 0) so the construction
     * emerges organically from around the laser connector.
     */
    fun generatePositions(radiusX: Int, radiusZ: Int, shape: FloorShape): List<BlockPos> {
        val list = mutableListOf<BlockPos>()
        val rx = radiusX.coerceIn(1, 32)
        val rz = radiusZ.coerceIn(1, 32)

        for (dx in -rx..rx) {
            for (dz in -rz..rz) {
                // (0, 0) is the location of the target laser connector itself; skip it
                if (dx == 0 && dz == 0) continue

                val include = when (shape) {
                    FloorShape.SQUARE -> true
                    FloorShape.CIRCLE -> {
                        // Normalized elliptical / circular test
                        val normX = dx.toDouble() / rx.toDouble()
                        val normZ = dz.toDouble() / rz.toDouble()
                        normX * normX + normZ * normZ <= 1.05
                    }
                    FloorShape.HOLLOW_FRAME -> {
                        // Only the outermost border ring
                        dx == -rx || dx == rx || dz == -rz || dz == rz
                    }
                    FloorShape.CHECKERBOARD -> {
                        (kotlin.math.abs(dx) + kotlin.math.abs(dz)) % 2 == 0
                    }
                }

                if (include) {
                    list.add(BlockPos(dx, 0, dz))
                }
            }
        }

        // Sort from inner to outer by Euclidean distance, with angular tiebreaker for clean spiral
        list.sortBy { pos ->
            val distSq = pos.x * pos.x + pos.z * pos.z
            val angle = kotlin.math.atan2(pos.z.toDouble(), pos.x.toDouble())
            distSq.toDouble() + (angle / 100.0)
        }

        return list
    }
}
