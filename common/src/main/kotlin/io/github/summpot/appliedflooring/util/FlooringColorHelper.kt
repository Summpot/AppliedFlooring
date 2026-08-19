package io.github.summpot.appliedflooring.util

import appeng.api.util.AEColor
import net.minecraft.world.item.DyeColor

object FlooringColorHelper {
    fun fromDyeColor(dye: DyeColor?): AEColor {
        if (dye == null) return AEColor.TRANSPARENT
        return AEColor.fromDye(dye) ?: AEColor.TRANSPARENT
    }

    fun toDyeColor(color: AEColor): DyeColor? {
        return when (color) {
            AEColor.WHITE -> DyeColor.WHITE
            AEColor.ORANGE -> DyeColor.ORANGE
            AEColor.MAGENTA -> DyeColor.MAGENTA
            AEColor.LIGHT_BLUE -> DyeColor.LIGHT_BLUE
            AEColor.YELLOW -> DyeColor.YELLOW
            AEColor.LIME -> DyeColor.LIME
            AEColor.PINK -> DyeColor.PINK
            AEColor.GRAY -> DyeColor.GRAY
            AEColor.LIGHT_GRAY -> DyeColor.LIGHT_GRAY
            AEColor.CYAN -> DyeColor.CYAN
            AEColor.PURPLE -> DyeColor.PURPLE
            AEColor.BLUE -> DyeColor.BLUE
            AEColor.BROWN -> DyeColor.BROWN
            AEColor.GREEN -> DyeColor.GREEN
            AEColor.RED -> DyeColor.RED
            AEColor.BLACK -> DyeColor.BLACK
            AEColor.TRANSPARENT -> null
        }
    }
}
