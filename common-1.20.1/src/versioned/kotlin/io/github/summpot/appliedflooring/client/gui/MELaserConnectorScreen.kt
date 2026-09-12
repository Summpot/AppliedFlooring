package io.github.summpot.appliedflooring.client.gui

import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.blockentity.BuilderStatus
import io.github.summpot.appliedflooring.blockentity.FloorShape
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import io.github.summpot.appliedflooring.blockentity.ReplaceMode
import io.github.summpot.appliedflooring.config.AppliedFlooringConfig
import io.github.summpot.appliedflooring.menu.MELaserConnectorMenu
import io.github.summpot.appliedflooring.network.LaserConnectorNetwork
import io.github.summpot.appliedflooring.util.LaserColumn
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory

class MELaserConnectorScreen(
    menu: MELaserConnectorMenu,
    playerInventory: Inventory,
    title: Component
) : AbstractContainerScreen<MELaserConnectorMenu>(menu, playerInventory, title) {

    private enum class ViewMode { OVERVIEW, BUILDER }

    private val openedEntity: MELaserConnectorBlockEntity?
        get() = menu.blockEntity

    private var viewMode = ViewMode.OVERVIEW
    private var selectedPos: BlockPos? = null
    private var listOffset = 0

    init {
        imageWidth = 244
        imageHeight = 222
    }

    override fun init() {
        super.init()
        if (selectedPos == null) {
            selectedPos = menu.blockPos
        }
        buildCustomWidgets()
    }

    private fun colorTranslationKey(color: AEColor): String {
        return if (color == AEColor.TRANSPARENT) {
            "gui.appliedflooring.color.transparent"
        } else {
            "gui.appliedflooring.color." + color.name.lowercase()
        }
    }

    private data class LayerEntry(
        val y: Int,
        val pos: BlockPos,
        val be: MELaserConnectorBlockEntity?,
        val planned: Boolean,
        val current: Boolean
    )

    private fun collectLayers(): List<LayerEntry> {
        val opened = openedEntity ?: return emptyList()
        val level = minecraft?.level ?: return emptyList()
        val connectors = LaserColumn.collectConnectors(level, menu.blockPos)
        val list = connectors.map { be ->
            LayerEntry(
                y = be.blockPos.y,
                pos = be.blockPos,
                be = be,
                planned = false,
                current = be.blockPos == menu.blockPos
            )
        }.toMutableList()

        if (list.isEmpty()) {
            list.add(LayerEntry(opened.blockPos.y, opened.blockPos, opened, planned = false, current = true))
        }

        val plannedY = opened.blockPos.y + opened.targetYOffset
        if (opened.targetYOffset >= 2 && list.none { it.y == plannedY }) {
            val plannedPos = opened.blockPos.atY(plannedY)
            list.add(
                LayerEntry(
                    y = plannedY,
                    pos = plannedPos,
                    be = null,
                    planned = true,
                    current = false
                )
            )
            list.sortBy { it.y }
        }
        return list
    }

    private fun selectedLayer(layers: List<LayerEntry>): LayerEntry? {
        val pos = selectedPos
        return layers.firstOrNull { it.pos == pos } ?: layers.firstOrNull { it.current } ?: layers.firstOrNull()
    }

    private fun actionTarget(layers: List<LayerEntry>): BlockPos {
        val selected = selectedLayer(layers)
        return selected?.be?.blockPos ?: menu.blockPos
    }

    private fun builderEntity(layers: List<LayerEntry>): MELaserConnectorBlockEntity? {
        val selected = selectedLayer(layers)
        return selected?.be ?: openedEntity
    }

    private fun buildCustomWidgets() {
        clearWidgets()
        val layers = collectLayers()
        if (selectedPos == null || layers.none { it.pos == selectedPos }) {
            selectedPos = selectedLayer(layers)?.pos ?: menu.blockPos
        }
        when (viewMode) {
            ViewMode.OVERVIEW -> buildOverviewWidgets(layers)
            ViewMode.BUILDER -> buildBuilderWidgets(layers)
        }
    }

    private fun buildOverviewWidgets(layers: List<LayerEntry>) {
        val x0 = leftPos
        val y0 = topPos
        val visible = 6
        if (listOffset > (layers.size - visible).coerceAtLeast(0)) {
            listOffset = (layers.size - visible).coerceAtLeast(0)
        }

        addRenderableWidget(
            Button.builder(Component.literal("▲")) {
                listOffset = (listOffset - 1).coerceAtLeast(0)
                buildCustomWidgets()
            }.bounds(x0 + 214, y0 + 78, 18, 16).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("▼")) {
                listOffset = (listOffset + 1).coerceAtMost((layers.size - visible).coerceAtLeast(0))
                buildCustomWidgets()
            }.bounds(x0 + 214, y0 + 168, 18, 16).build()
        )

        val slice = layers.drop(listOffset).take(visible)
        for (i in slice.indices) {
            val entry = slice[i]
            val label = if (entry.planned) {
                Component.translatable("gui.appliedflooring.laser_connector.layer_planned_row", entry.y)
            } else {
                Component.translatable("gui.appliedflooring.laser_connector.layer_row", entry.y)
            }
            val btn = Button.builder(label) {
                selectedPos = entry.pos
                buildCustomWidgets()
            }.bounds(x0 + 14, y0 + 78 + i * 18, 196, 16).build()
            addRenderableWidget(btn)
        }

        addRenderableWidget(
            Button.builder(Component.translatable("gui.appliedflooring.laser_connector.configure")) {
                viewMode = ViewMode.BUILDER
                buildCustomWidgets()
            }.bounds(x0 + 14, y0 + 192, 104, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.laser_connector.configure.tooltip")))
                .build()
        )

        val selected = selectedLayer(layers)
        val opened = openedEntity
        val placeTarget = when {
            selected?.planned == true -> selected.pos
            opened != null && opened.targetYOffset >= 2 -> opened.blockPos.above(opened.targetYOffset)
            else -> null
        }
        val level = minecraft?.level
        val canPlace = placeTarget != null && opened != null && opened.connectedTargetUp == null && opened.targetYOffset >= 2 &&
            (level?.getBlockState(placeTarget)?.let { it.isAir || it.canBeReplaced() } ?: true)
        val placeBtn = Button.builder(Component.translatable("gui.appliedflooring.laser_connector.place_remote")) {
            LaserConnectorNetwork.sendAction(menu.blockPos, LaserConnectorNetwork.ACTION_PLACE_REMOTE_CONNECTOR)
        }.bounds(x0 + 126, y0 + 192, 104, 20)
            .tooltip(
                Tooltip.create(
                    Component.translatable(
                        "gui.appliedflooring.laser_connector.place_remote.tooltip",
                        placeTarget?.y ?: (menu.blockPos.y + (opened?.targetYOffset ?: 0))
                    )
                )
            )
            .build()
        placeBtn.active = canPlace
        addRenderableWidget(placeBtn)
    }

    private fun buildBuilderWidgets(layers: List<LayerEntry>) {
        val be = builderEntity(layers) ?: return
        val x0 = leftPos
        val y0 = topPos
        val pos = actionTarget(layers)

        addRenderableWidget(
            Button.builder(Component.translatable("gui.appliedflooring.laser_connector.back")) {
                viewMode = ViewMode.OVERVIEW
                buildCustomWidgets()
            }.bounds(x0 + 12, y0 + 22, 48, 16).build()
        )

        addRenderableWidget(
            Button.builder(Component.literal("-5")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset - 5)
            }.bounds(x0 + 12, y0 + 42, 22, 16).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("-1")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset - 1)
            }.bounds(x0 + 36, y0 + 42, 18, 16).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("+1")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset + 1)
            }.bounds(x0 + 56, y0 + 42, 18, 16).build()
        )
        addRenderableWidget(
            Button.builder(Component.literal("+5")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset + 5)
            }.bounds(x0 + 76, y0 + 42, 22, 16).build()
        )

        val targetPos = be.blockPos.above(be.targetYOffset)
        val level = minecraft?.level
        val isTargetClear = be.targetYOffset >= 2 && (level?.let {
            val state = it.getBlockState(targetPos)
            state.isAir || state.canBeReplaced()
        } ?: true)
        val placeBtn = Button.builder(Component.translatable("gui.appliedflooring.laser_connector.place_remote")) {
            LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_PLACE_REMOTE_CONNECTOR)
        }.bounds(x0 + 104, y0 + 40, 128, 20)
            .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.laser_connector.place_remote.tooltip", targetPos.y)))
            .build()
        placeBtn.active = isTargetClear && be.connectedTargetUp == null && be.targetYOffset >= 2
        addRenderableWidget(placeBtn)

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

        val colors = AEColor.values()
        addRenderableWidget(
            Button.builder(Component.translatable("gui.appliedflooring.laser_connector.color", Component.translatable(colorTranslationKey(be.floorColor)))) {
                val nextIdx = (be.floorColor.ordinal + 1) % colors.size
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_COLOR, nextIdx)
            }.bounds(x0 + 106, y0 + 104, 72, 16)
                .tooltip(Tooltip.create(Component.translatable("gui.appliedflooring.laser_connector.color.tooltip")))
                .build()
        )

        addRenderableWidget(
            Button.builder(be.replaceMode.getButtonText()) {
                val nextMode = if (be.replaceMode == ReplaceMode.AIR_ONLY) ReplaceMode.REPLACE_ALL else ReplaceMode.AIR_ONLY
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_REPLACE_MODE, nextMode.id)
            }.bounds(x0 + 182, y0 + 104, 50, 16)
                .tooltip(Tooltip.create(be.replaceMode.getTooltipText()))
                .build()
        )

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
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, -0x1000000)
        guiGraphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, -0xdad9d7)

        if (viewMode == ViewMode.OVERVIEW) {
            renderPanel(guiGraphics, x + 8, y + 22, imageWidth - 16, 48)
            renderPanel(guiGraphics, x + 8, y + 74, imageWidth - 16, 112)
            val layers = collectLayers()
            val selected = selectedLayer(layers)
            val visible = 6
            val slice = layers.drop(listOffset).take(visible)
            for (i in slice.indices) {
                if (slice[i].pos == selected?.pos) {
                    val rowY = y + 78 + i * 18
                    guiGraphics.fill(x + 14, rowY, x + 210, rowY + 16, 0xFF005A80.toInt())
                }
            }
            return
        }

        renderPanel(guiGraphics, x + 8, y + 22, imageWidth - 16, 42)
        renderPanel(guiGraphics, x + 8, y + 68, imageWidth - 16, 58)
        renderPanel(guiGraphics, x + 8, y + 130, imageWidth - 16, 86)

        val be = builderEntity(collectLayers()) ?: return
        val barX = x + 14
        val barY = y + 168
        val barWidth = imageWidth - 28
        val barHeight = 16
        guiGraphics.fill(barX, barY, barX + barWidth, barY + barHeight, -0xefeeed)
        if (be.totalBlocksCount > 0) {
            val progress = (be.placedBlocksCount.toFloat() / be.totalBlocksCount.toFloat()).coerceIn(0.0f, 1.0f)
            val fillWidth = (barWidth * progress).toInt()
            if (fillWidth > 0) {
                guiGraphics.fill(barX, barY, barX + fillWidth, barY + barHeight, -0xff4c01)
            }
        }
    }

    private fun renderPanel(guiGraphics: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        guiGraphics.fill(x, y, x + w, y + h, -0xc5c4c2)
        guiGraphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, -0xe1e0de)
    }

    override fun renderLabels(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int) {
        val opened = openedEntity ?: return
        val title = Component.translatable("gui.appliedflooring.laser_connector.title")
        guiGraphics.drawString(font, title, 10, 8, 0xE0E6ED, false)

        val isNetworkPowered = menu.isPowered || opened.isPowered()
        val powerColor = if (isNetworkPowered) 0x00E676 else 0xFF5252
        val powerText = if (isNetworkPowered) {
            Component.translatable("gui.appliedflooring.laser_connector.power_online")
        } else {
            Component.translatable("gui.appliedflooring.laser_connector.power_offline")
        }
        guiGraphics.drawString(font, powerText, imageWidth - font.width(powerText) - 10, 8, powerColor, false)

        if (viewMode == ViewMode.OVERVIEW) {
            renderOverviewLabels(guiGraphics)
        } else {
            renderBuilderLabels(guiGraphics)
        }
    }

    private fun renderOverviewLabels(guiGraphics: GuiGraphics) {
        val layers = collectLayers()
        val linked = layers.count { it.be?.connectedTargetUp != null }
        val building = layers.count { it.be?.builderStatus == BuilderStatus.BUILDING }
        val drain = layers.sumOf { beLayer ->
            val target = beLayer.be?.connectedTargetUp ?: return@sumOf 0.0
            val dist = target.y - beLayer.y
            AppliedFlooringConfig.laserIdleBaseEnergy + dist * AppliedFlooringConfig.laserIdleDistanceEnergy
        }
        val placed = layers.sumOf { it.be?.placedBlocksCount ?: 0 }
        val total = layers.sumOf { it.be?.totalBlocksCount ?: 0 }

        guiGraphics.drawString(
            font,
            Component.translatable("gui.appliedflooring.laser_connector.layers", layers.size),
            12, 28, 0x00D2FF, false
        )
        guiGraphics.drawString(
            font,
            Component.translatable("gui.appliedflooring.laser_connector.connections", linked),
            90, 28, 0xE0E6ED, false
        )
        guiGraphics.drawString(
            font,
            Component.translatable("gui.appliedflooring.laser_connector.building_count", building),
            160, 28, if (building > 0) 0x00D2FF else 0x8C9BAE, false
        )
        guiGraphics.drawString(
            font,
            Component.translatable("gui.appliedflooring.laser_connector.idle_energy", "%.1f".format(drain)),
            12, 42, 0xE0E6ED, false
        )
        val percent = if (total > 0) ((placed.toDouble() / total) * 100).toInt() else 0
        guiGraphics.drawString(
            font,
            Component.translatable("gui.appliedflooring.laser_connector.stack_progress", placed, total, percent),
            12, 54, 0xE0E6ED, false
        )
    }

    private fun renderBuilderLabels(guiGraphics: GuiGraphics) {
        val layers = collectLayers()
        val be = builderEntity(layers) ?: return
        val targetY = be.resolveBuildY()
        val yInfo = Component.translatable("gui.appliedflooring.laser_connector.build_y", targetY, be.targetYOffset)
        guiGraphics.drawString(font, yInfo, 64, 26, 0x00D2FF, false)

        val (targetStatusText, targetStatusColor) = if (be.connectedTargetUp != null) {
            Component.translatable("gui.appliedflooring.laser_connector.target_connected") to 0x00E676
        } else {
            val level = minecraft?.level
            val checkPos = be.blockPos.above(be.targetYOffset.coerceAtLeast(0))
            val st = level?.getBlockState(checkPos)
            if (be.targetYOffset == 0) {
                Component.translatable("gui.appliedflooring.laser_connector.target_self") to 0x00D2FF
            } else if (st == null || st.isAir || st.canBeReplaced()) {
                Component.translatable("gui.appliedflooring.laser_connector.target_available") to 0xFFB300
            } else {
                Component.translatable("gui.appliedflooring.laser_connector.target_blocked") to 0xFF5252
            }
        }
        guiGraphics.drawString(font, targetStatusText, 104, 42, targetStatusColor, false)

        val radiusText = Component.translatable("gui.appliedflooring.laser_connector.radius", be.radiusX, be.radiusX * 2 + 1, be.radiusX * 2 + 1)
        guiGraphics.drawString(font, radiusText, 32, 108, 0xE0E6ED, false)

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
