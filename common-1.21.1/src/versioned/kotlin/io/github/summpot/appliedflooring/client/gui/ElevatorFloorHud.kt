package io.github.summpot.appliedflooring.client.gui

import io.github.summpot.appliedflooring.block.MEElevatorBlock
import io.github.summpot.appliedflooring.network.ElevatorNetwork
import io.github.summpot.appliedflooring.util.ElevatorTeleportHelper
import io.github.summpot.appliedflooring.util.isOnGroundCompat
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component

object ElevatorFloorHud {
    private const val HOLD_TICKS = 6

    private var holding = false
    private var heldTicks = 0
    private var pickerOpen = false
    private var origin: BlockPos? = null
    private var floors: List<BlockPos> = emptyList()
    private var selectedIndex = 0

    fun tick() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return reset()
        val level = player.level()
        if (player.isSpectator) return reset()

        val below = player.blockPosition().below()
        val onElevator = level.getBlockState(below).block is MEElevatorBlock
        val sneaking = player.isShiftKeyDown
        val onGround = player.isOnGroundCompat()

        if (onElevator && sneaking && onGround) {
            if (!holding || origin != below) {
                holding = true
                heldTicks = 0
                pickerOpen = false
                origin = below
                floors = ElevatorTeleportHelper.collectFloors(level, below, false)
                selectedIndex = floors.indexOf(below).coerceAtLeast(0)
            }
            heldTicks++
            if (heldTicks >= HOLD_TICKS) {
                pickerOpen = true
            }
            return
        }

        if (holding && !sneaking) {
            if (pickerOpen) {
                val dest = floors.getOrNull(selectedIndex)
                val from = origin
                if (dest != null && from != null && dest != from) {
                    ElevatorNetwork.sendTeleport(dest)
                }
            } else {
                ElevatorNetwork.sendGoDown()
            }
        }
        reset()
    }

    fun onScroll(amount: Double): Boolean {
        if (!holding || floors.isEmpty()) return false
        pickerOpen = true
        if (amount < 0) {
            selectedIndex = (selectedIndex + 1).coerceAtMost(floors.lastIndex)
        } else if (amount > 0) {
            selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
        }
        return true
    }

    fun render(guiGraphics: GuiGraphics) {
        if (!pickerOpen || floors.isEmpty()) return
        val mc = Minecraft.getInstance()
        val font = mc.font
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight

        val panelW = 150
        val rowH = 12
        val headerH = 28
        val visible = floors.size.coerceAtMost(12)
        val panelH = headerH + visible * rowH + 8
        val x = screenW - panelW - 8
        val y = (screenH - panelH) / 2

        guiGraphics.fill(x, y, x + panelW, y + panelH, 0xC0101113.toInt())
        guiGraphics.fill(x + 1, y + 1, x + panelW - 1, y + panelH - 1, 0xE01E1F22.toInt())

        val title = Component.translatable("gui.appliedflooring.elevator.select_title")
        guiGraphics.drawString(font, title, x + 8, y + 6, 0x00D2FF, false)
        val hint = Component.translatable("gui.appliedflooring.elevator.select_hint")
        guiGraphics.drawString(font, hint, x + 8, y + 16, 0x8C9BAE, false)

        val current = origin
        val start = (selectedIndex - visible / 2).coerceIn(0, (floors.size - visible).coerceAtLeast(0))
        for (i in 0 until visible) {
            val idx = start + i
            if (idx >= floors.size) break
            val floor = floors[idx]
            val rowY = y + headerH + i * rowH
            val selected = idx == selectedIndex
            if (selected) {
                guiGraphics.fill(x + 3, rowY - 1, x + panelW - 3, rowY + rowH - 1, 0xFF005A80.toInt())
            }
            val isCurrent = floor == current
            val color = when {
                selected -> 0xE0E6ED
                isCurrent -> 0x00E676
                else -> 0x8C9BAE
            }
            val label = Component.translatable("gui.appliedflooring.elevator.floor_label", idx + 1, floor.y)
            val text = if (isCurrent) {
                Component.translatable("gui.appliedflooring.elevator.floor_current", label)
            } else {
                label
            }
            guiGraphics.drawString(font, text, x + 8, rowY, color, false)
        }
    }

    private fun reset() {
        holding = false
        heldTicks = 0
        pickerOpen = false
        origin = null
        floors = emptyList()
        selectedIndex = 0
    }
}
