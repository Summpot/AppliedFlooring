package io.github.summpot.appliedflooring.client.gui

import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.blockentity.BuilderStatus
import io.github.summpot.appliedflooring.blockentity.FloorShape
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import io.github.summpot.appliedflooring.blockentity.ReplaceMode
import io.github.summpot.appliedflooring.menu.MELaserConnectorMenu
import io.github.summpot.appliedflooring.network.LaserConnectorNetwork
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class MELaserConnectorScreen(
    menu: MELaserConnectorMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<MELaserConnectorMenu>(menu, playerInventory, title) {

    private val blockEntity: MELaserConnectorBlockEntity?
        get() = menu.blockEntity

    init {
        imageWidth = 244
        imageHeight = 222
    }

    override fun init() {
        super.init()
        buildCustomWidgets()
    }

    private fun colorTranslationKey(color: AEColor): String {
        return if (color == AEColor.TRANSPARENT) {
            "gui.appliedflooring.color.transparent"
        } else {
            "gui.appliedflooring.color." + color.name.lowercase()
        }
    }

    private fun buildCustomWidgets() {
        clearWidgets()
        val be = blockEntity ?: return
        val x0 = leftPos
        val y0 = topPos
        val pos = menu.blockPos

        // --- Section 1: Y-Offset Buttons ---
        addRenderableWidget(
            Button.builder(Component.literal("-5")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset - 5)
            }.bounds(x0 + 12, y0 + 38, 22, 16).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("-1")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset - 1)
            }.bounds(x0 + 36, y0 + 38, 18, 16).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("+1")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset + 1)
            }.bounds(x0 + 56, y0 + 38, 18, 16).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("+5")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset + 5)
            }.bounds(x0 + 76, y0 + 38, 22, 16).build()
        )

        // Remote Place Connector Button
        val targetPos = pos.above(be.targetYOffset)
        val level = minecraft?.level
        val isTargetClear = level?.let {
            val state = it.getBlockState(targetPos)
            state.isAir || state.canBeReplaced()
        } ?: true
        val isAlreadyConnected = be.connectedTargetUp != null

        val placeBtn = Button.builder(Component.translatable("gui.appliedflooring.laser_connector.place_remote")) {
            LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_PLACE_REMOTE_CONNECTOR)
        }.bounds(x0 + 104, y0 + 36, 128, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.laser_connector.place_remote.tooltip", targetPos.y)))
            .build()
        placeBtn.active = isTargetClear && !isAlreadyConnected
        addRenderableWidget(placeBtn)

        // --- Section 2: Shapes ---
        val shapes = FloorShape.values()
        val shapeWidth = 52
        for (i in shapes.indices) {
            val s = shapes[i]
            val btn = Button.builder(s.getDisplayName()) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_SHAPE, s.id)
            }.bounds(x0 + 12 + i * 56, y0 + 82, shapeWidth, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.shape.tooltip", s.getDisplayName())))
                .build()
            addRenderableWidget(btn)
        }

        // Radius Stepper
        addRenderableWidget(
            Button.builder(Component.literal("-")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_RADIUS, be.radiusX - 1, be.radiusZ - 1)
            }.bounds(x0 + 12, y0 + 104, 16, 16).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("+")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_RADIUS, be.radiusX + 1, be.radiusZ + 1)
            }.bounds(x0 + 86, y0 + 104, 16, 16).build()
        )

        // Color Cycle Button
        val colors = AEColor.values()
        addRenderableWidget(
            Button.builder(Component.translatable("gui.appliedflooring.laser_connector.color", Component.translatable(colorTranslationKey(be.floorColor)))) {
                val nextIdx = (be.floorColor.ordinal + 1) % colors.size
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_COLOR, nextIdx)
            }.bounds(x0 + 106, y0 + 104, 72, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.laser_connector.color.tooltip")))
                .build()
        )

        // Replace Mode Button
        addRenderableWidget(
            Button.builder(be.replaceMode.getButtonText()) {
                val nextMode = if (be.replaceMode == ReplaceMode.AIR_ONLY) ReplaceMode.REPLACE_ALL else ReplaceMode.AIR_ONLY
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_REPLACE_MODE, nextMode.id)
            }.bounds(x0 + 182, y0 + 104, 50, 16)
                .tooltip(Tooltip.create(be.replaceMode.getTooltipText()))
                .build()
        )

        // --- Section 3: Build Action Controls ---
        if (be.builderStatus == BuilderStatus.BUILDING) {
            addRenderableWidget(
                Button.builder(Component.translatable("gui.appliedflooring.laser_connector.pause")) {
                    LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_PAUSE_BUILD)
                }.bounds(x0 + 14, y0 + 192, 104, 20)
                    .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.laser_connector.pause.tooltip")))
                    .build()
            )
        } else {
            val startBtn = Button.builder(Component.translatable("gui.appliedflooring.laser_connector.start")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_START_BUILD)
            }.bounds(x0 + 14, y0 + 192, 104, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.laser_connector.start.tooltip")))
                .build()
            startBtn.active = menu.isPowered || be.isPowered()
            addRenderableWidget(startBtn)
        }

        addRenderableWidget(
            Button.builder(Component.translatable("gui.appliedflooring.laser_connector.reset")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_RESET_BUILD)
            }.bounds(x0 + 126, y0 + 192, 104, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.laser_connector.reset.tooltip")))
                .build()
        )
    }

    override fun containerTick() {
        super.containerTick()
        if (minecraft?.player?.tickCount?.rem(5) == 0) {
            buildCustomWidgets()
        }
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(guiGraphics)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
        renderTooltip(guiGraphics, mouseX, mouseY)
    }

    override fun renderBg(guiGraphics: GuiGraphics, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos
        val y = topPos

        // Outer Dark Panel
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, -0x1000000) // 0xFF000000
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, -0xdad9d7) // 0xFF252629

        // Section 1: Vertical Layer
        renderPanel(guiGraphics, x + 8, y + 22, imageWidth - 16, 38)

        // Section 2: Floor Shape & Radius
        renderPanel(guiGraphics, x + 8, y + 64, imageWidth - 16, 62)

        // Section 3: Engine Status & Action Controls
        renderPanel(guiGraphics, x + 8, y + 130, imageWidth - 16, 86)

        // Progress Bar Background & Fill
        val be = blockEntity ?: return
        val barX = x + 14
        val barY = y + 168
        val barWidth = imageWidth - 28
        val barHeight = 16

        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, -0xefeeed) // 0xFF101113
        if (be.totalBlocksCount > 0) {
            val progress = (be.placedBlocksCount.toFloat() / be.totalBlocksCount.toFloat()).coerceIn(0.0f, 1.0f)
            val fillWidth = (barWidth * progress).toInt()
            if (fillWidth > 0) {
                guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, -0xff4c01) // 0xFF00B3FF
            }
        }
    }

    private fun renderPanel(guiGraphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        guiGraphics.fill(x, y, x + w, y + h, -0xc5c4c2) // 0xFF3A3B3E
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, -0xe1e0de) // 0xFF1E1F22
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val be = blockEntity ?: return

        // 1. Header Title & Power Indicator
        val title = Component.translatable("gui.appliedflooring.laser_connector.title")
        guiGraphics.drawString(font, title, 10, 8, 0xE0E6ED, false)

        val isNetworkPowered = menu.isPowered || be.isPowered()
        val powerColor = if (isNetworkPowered) 0x00E676 else 0xFF5252
        val powerText = if (isNetworkPowered) {
            Component.translatable("gui.appliedflooring.laser_connector.power_online")
        } else {
            Component.translatable("gui.appliedflooring.laser_connector.power_offline")
        }
        guiGraphics.drawString(font, powerText, imageWidth - font.width(powerText) - 10, 8, powerColor, false)

        // 2. Section 1 Labels
        val pos = menu.blockPos
        val targetY = pos.y + be.targetYOffset
        val yInfo = Component.translatable("gui.appliedflooring.laser_connector.target_y", targetY, be.targetYOffset)
        guiGraphics.drawString(font, yInfo, 12, 28, 0x00D2FF, false)

        val (targetStatusText, targetStatusColor) = if (be.connectedTargetUp != null) {
            Component.translatable("gui.appliedflooring.laser_connector.target_connected") to 0x00E676
        } else {
            val level = minecraft?.level
            val checkPos = pos.above(be.targetYOffset)
            val st = level?.getBlockState(checkPos)
            if (st == null || st.isAir || st.canBeReplaced()) {
                Component.translatable("gui.appliedflooring.laser_connector.target_available") to 0xFFB300
            } else {
                Component.translatable("gui.appliedflooring.laser_connector.target_blocked") to 0xFF5252
            }
        }
        guiGraphics.drawString(font, targetStatusText, 104, 28, targetStatusColor, false)

        // 3. Section 2 Labels
        val radiusText = Component.translatable("gui.appliedflooring.laser_connector.radius", be.radiusX, be.radiusX * 2 + 1, be.radiusX * 2 + 1)
        guiGraphics.drawString(font, radiusText, 32, 108, 0xE0E6ED, false)

        // 4. Section 3 Labels (Progress & Status)
        val statusDesc = Component.translatable("gui.appliedflooring.laser_connector.status_label", be.builderStatus.getDisplayName())
        val statusColor = when (be.builderStatus) {
            BuilderStatus.BUILDING -> 0x00D2FF
            BuilderStatus.DONE -> 0x00E676
            BuilderStatus.PAUSED -> 0xFFB300
            BuilderStatus.NO_POWER, BuilderStatus.NO_ITEMS, BuilderStatus.BLOCKED -> 0xFF5252
            BuilderStatus.IDLE -> 0x8C9BAE
        }
        guiGraphics.drawString(font, statusDesc, 14, 138, statusColor, false)

        val percent = if (be.totalBlocksCount > 0) ((be.placedBlocksCount.toDouble() / be.totalBlocksCount) * 100).toInt() else 0
        val countText = Component.translatable("gui.appliedflooring.laser_connector.progress", be.placedBlocksCount, be.totalBlocksCount, percent)
        guiGraphics.drawString(font, countText, 14, 152, 0xE0E6ED, false)
    }
}
