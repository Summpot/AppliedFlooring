package io.github.summpot.appliedflooring.client.gui

import appeng.api.util.AEColor
import com.mojang.blaze3d.vertex.PoseStack
import io.github.summpot.appliedflooring.blockentity.BuilderStatus
import io.github.summpot.appliedflooring.blockentity.FloorShape
import io.github.summpot.appliedflooring.blockentity.MELaserConnectorBlockEntity
import io.github.summpot.appliedflooring.blockentity.ReplaceMode
import io.github.summpot.appliedflooring.config.AppliedFlooringConfig
import io.github.summpot.appliedflooring.menu.MELaserConnectorMenu
import io.github.summpot.appliedflooring.network.LaserConnectorNetwork
import io.github.summpot.appliedflooring.util.LaserColumn
import net.minecraft.client.gui.components.Button
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

    private fun makeTooltip(comp: Component): Button.OnTooltip {
        return Button.OnTooltip { _, poseStack, mouseX, mouseY ->
            renderTooltip(poseStack, comp, mouseX, mouseY)
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
            Button(x0 + 214, y0 + 78, 18, 16, Component.literal("▲")) {
                listOffset = (listOffset - 1).coerceAtLeast(0)
                buildCustomWidgets()
            }
        )
        addRenderableWidget(
            Button(x0 + 214, y0 + 168, 18, 16, Component.literal("▼")) {
                listOffset = (listOffset + 1).coerceAtMost((layers.size - visible).coerceAtLeast(0))
                buildCustomWidgets()
            }
        )

        val slice = layers.drop(listOffset).take(visible)
        for (i in slice.indices) {
            val entry = slice[i]
            val label = if (entry.planned) {
                Component.translatable("gui.appliedflooring.laser_connector.layer_planned_row", entry.y)
            } else {
                Component.translatable("gui.appliedflooring.laser_connector.layer_row", entry.y)
            }
            addRenderableWidget(
                Button(x0 + 14, y0 + 78 + i * 18, 196, 16, label) {
                    selectedPos = entry.pos
                    buildCustomWidgets()
                }
            )
        }

        addRenderableWidget(
            Button(
                x0 + 14, y0 + 192, 104, 20,
                Component.translatable("gui.appliedflooring.laser_connector.configure"),
                { viewMode = ViewMode.BUILDER; buildCustomWidgets() },
                makeTooltip(Component.translatable("gui.appliedflooring.laser_connector.configure.tooltip"))
            )
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
            (level?.getBlockState(placeTarget)?.let { it.isAir || it.material.isReplaceable } ?: true)
        val placeBtn = Button(
            x0 + 126, y0 + 192, 104, 20,
            Component.translatable("gui.appliedflooring.laser_connector.place_remote"),
            { LaserConnectorNetwork.sendAction(menu.blockPos, LaserConnectorNetwork.ACTION_PLACE_REMOTE_CONNECTOR) },
            makeTooltip(
                Component.translatable(
                    "gui.appliedflooring.laser_connector.place_remote.tooltip",
                    placeTarget?.y ?: (menu.blockPos.y + (opened?.targetYOffset ?: 0))
                )
            )
        )
        placeBtn.active = canPlace
        addRenderableWidget(placeBtn)
    }

    private fun buildBuilderWidgets(layers: List<LayerEntry>) {
        val be = builderEntity(layers) ?: return
        val x0 = leftPos
        val y0 = topPos
        val pos = actionTarget(layers)

        addRenderableWidget(
            Button(x0 + 12, y0 + 22, 48, 16, Component.translatable("gui.appliedflooring.laser_connector.back")) {
                viewMode = ViewMode.OVERVIEW
                buildCustomWidgets()
            }
        )

        addRenderableWidget(
            Button(x0 + 12, y0 + 42, 22, 16, Component.literal("-5")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset - 5)
            }
        )
        addRenderableWidget(
            Button(x0 + 36, y0 + 42, 18, 16, Component.literal("-1")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset - 1)
            }
        )
        addRenderableWidget(
            Button(x0 + 56, y0 + 42, 18, 16, Component.literal("+1")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset + 1)
            }
        )
        addRenderableWidget(
            Button(x0 + 76, y0 + 42, 22, 16, Component.literal("+5")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_TARGET_OFFSET, be.targetYOffset + 5)
            }
        )

        val targetPos = be.blockPos.above(be.targetYOffset)
        val level = minecraft?.level
        val isTargetClear = be.targetYOffset >= 2 && (level?.let {
            val state = it.getBlockState(targetPos)
            state.isAir || state.material.isReplaceable
        } ?: true)
        val placeBtn = Button(
            x0 + 104, y0 + 40, 128, 20,
            Component.translatable("gui.appliedflooring.laser_connector.place_remote"),
            { LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_PLACE_REMOTE_CONNECTOR) },
            makeTooltip(Component.translatable("gui.appliedflooring.laser_connector.place_remote.tooltip", targetPos.y))
        )
        placeBtn.active = isTargetClear && be.connectedTargetUp == null && be.targetYOffset >= 2
        addRenderableWidget(placeBtn)

        val shapes = FloorShape.values()
        val shapeWidth = 52
        for (i in shapes.indices) {
            val s = shapes[i]
            addRenderableWidget(
                Button(
                    x0 + 12 + i * 56, y0 + 82, shapeWidth, 16,
                    s.getDisplayName(),
                    { LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_SHAPE, s.id) },
                    makeTooltip(Component.translatable("gui.appliedflooring.shape.tooltip", s.getDisplayName()))
                )
            )
        }

        addRenderableWidget(
            Button(x0 + 12, y0 + 104, 16, 16, Component.literal("-")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_RADIUS, be.radiusX - 1, be.radiusZ - 1)
            }
        )
        addRenderableWidget(
            Button(x0 + 86, y0 + 104, 16, 16, Component.literal("+")) {
                LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_RADIUS, be.radiusX + 1, be.radiusZ + 1)
            }
        )

        val colors = AEColor.values()
        addRenderableWidget(
            Button(
                x0 + 106, y0 + 104, 72, 16,
                Component.translatable("gui.appliedflooring.laser_connector.color", Component.translatable(colorTranslationKey(be.floorColor))),
                {
                    val nextIdx = (be.floorColor.ordinal + 1) % colors.size
                    LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_COLOR, nextIdx)
                },
                makeTooltip(Component.translatable("gui.appliedflooring.laser_connector.color.tooltip"))
            )
        )

        addRenderableWidget(
            Button(
                x0 + 182, y0 + 104, 50, 16,
                be.replaceMode.getButtonText(),
                {
                    val nextMode = if (be.replaceMode == ReplaceMode.AIR_ONLY) ReplaceMode.REPLACE_ALL else ReplaceMode.AIR_ONLY
                    LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_SET_REPLACE_MODE, nextMode.id)
                },
                makeTooltip(be.replaceMode.getTooltipText())
            )
        )

        if (be.builderStatus == BuilderStatus.BUILDING) {
            addRenderableWidget(
                Button(
                    x0 + 14, y0 + 192, 104, 20,
                    Component.translatable("gui.appliedflooring.laser_connector.pause"),
                    { LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_PAUSE_BUILD) },
                    makeTooltip(Component.translatable("gui.appliedflooring.laser_connector.pause.tooltip"))
                )
            )
        } else {
            val startBtn = Button(
                x0 + 14, y0 + 192, 104, 20,
                Component.translatable("gui.appliedflooring.laser_connector.start"),
                { LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_START_BUILD) },
                makeTooltip(Component.translatable("gui.appliedflooring.laser_connector.start.tooltip"))
            )
            startBtn.active = menu.isPowered || be.isPowered()
            addRenderableWidget(startBtn)
        }

        addRenderableWidget(
            Button(
                x0 + 126, y0 + 192, 104, 20,
                Component.translatable("gui.appliedflooring.laser_connector.reset"),
                { LaserConnectorNetwork.sendAction(pos, LaserConnectorNetwork.ACTION_RESET_BUILD) },
                makeTooltip(Component.translatable("gui.appliedflooring.laser_connector.reset.tooltip"))
            )
        )
    }

    override fun containerTick() {
        super.containerTick()
        if (minecraft?.player?.tickCount?.rem(5) == 0) {
            buildCustomWidgets()
        }
    }

    override fun render(poseStack: PoseStack, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(poseStack)
        super.render(poseStack, mouseX, mouseY, partialTick)
        renderTooltip(poseStack, mouseX, mouseY)
    }

    override fun renderBg(poseStack: PoseStack, partialTick: Float, mouseX: Int, mouseY: Int) {
        val x = leftPos
        val y = topPos
        fill(poseStack, x, y, x + imageWidth, y + imageHeight, -0x1000000)
        fill(poseStack, x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, -0xdad9d7)

        if (viewMode == ViewMode.OVERVIEW) {
            renderPanel(poseStack, x + 8, y + 22, imageWidth - 16, 48)
            renderPanel(poseStack, x + 8, y + 74, imageWidth - 16, 112)
            val layers = collectLayers()
            val selected = selectedLayer(layers)
            val visible = 6
            val slice = layers.drop(listOffset).take(visible)
            for (i in slice.indices) {
                if (slice[i].pos == selected?.pos) {
                    val rowY = y + 78 + i * 18
                    fill(poseStack, x + 14, rowY, x + 210, rowY + 16, 0xFF005A80.toInt())
                }
            }
            return
        }

        renderPanel(poseStack, x + 8, y + 22, imageWidth - 16, 42)
        renderPanel(poseStack, x + 8, y + 68, imageWidth - 16, 58)
        renderPanel(poseStack, x + 8, y + 130, imageWidth - 16, 86)

        val be = builderEntity(collectLayers()) ?: return
        val barX = x + 14
        val barY = y + 168
        val barWidth = imageWidth - 28
        val barHeight = 16
        fill(poseStack, barX, barY, barX + barWidth, barY + barHeight, -0xefeeed)
        if (be.totalBlocksCount > 0) {
            val progress = (be.placedBlocksCount.toFloat() / be.totalBlocksCount.toFloat()).coerceIn(0.0f, 1.0f)
            val fillWidth = (barWidth * progress).toInt()
            if (fillWidth > 0) {
                fill(poseStack, barX, barY, barX + fillWidth, barY + barHeight, -0xff4c01)
            }
        }
    }

    private fun renderPanel(poseStack: PoseStack, x: Int, y: Int, w: Int, h: Int) {
        fill(poseStack, x, y, x + w, y + h, -0xc5c4c2)
        fill(poseStack, x + 1, y + 1, x + w - 1, y + h - 1, -0xe1e0de)
    }

    override fun renderLabels(poseStack: PoseStack, mouseX: Int, mouseY: Int) {
        val opened = openedEntity ?: return
        val title = Component.translatable("gui.appliedflooring.laser_connector.title")
        font.draw(poseStack, title, 10.0f, 8.0f, 0xE0E6ED)

        val isNetworkPowered = menu.isPowered || opened.isPowered()
        val powerColor = if (isNetworkPowered) 0x00E676 else 0xFF5252
        val powerText = if (isNetworkPowered) {
            Component.translatable("gui.appliedflooring.laser_connector.power_online")
        } else {
            Component.translatable("gui.appliedflooring.laser_connector.power_offline")
        }
        font.draw(poseStack, powerText, (imageWidth - font.width(powerText) - 10).toFloat(), 8.0f, powerColor)

        if (viewMode == ViewMode.OVERVIEW) {
            renderOverviewLabels(poseStack)
        } else {
            renderBuilderLabels(poseStack)
        }
    }

    private fun renderOverviewLabels(poseStack: PoseStack) {
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

        font.draw(poseStack, Component.translatable("gui.appliedflooring.laser_connector.layers", layers.size), 12.0f, 28.0f, 0x00D2FF)
        font.draw(poseStack, Component.translatable("gui.appliedflooring.laser_connector.connections", linked), 90.0f, 28.0f, 0xE0E6ED)
        font.draw(poseStack, Component.translatable("gui.appliedflooring.laser_connector.building_count", building), 160.0f, 28.0f, if (building > 0) 0x00D2FF else 0x8C9BAE)
        font.draw(poseStack, Component.translatable("gui.appliedflooring.laser_connector.idle_energy", "%.1f".format(drain)), 12.0f, 42.0f, 0xE0E6ED)
        val percent = if (total > 0) ((placed.toDouble() / total) * 100).toInt() else 0
        font.draw(poseStack, Component.translatable("gui.appliedflooring.laser_connector.stack_progress", placed, total, percent), 12.0f, 54.0f, 0xE0E6ED)
    }

    private fun renderBuilderLabels(poseStack: PoseStack) {
        val layers = collectLayers()
        val be = builderEntity(layers) ?: return
        val targetY = be.resolveBuildY()
        val yInfo = Component.translatable("gui.appliedflooring.laser_connector.build_y", targetY, be.targetYOffset)
        font.draw(poseStack, yInfo, 64.0f, 26.0f, 0x00D2FF)

        val (targetStatusText, targetStatusColor) = if (be.connectedTargetUp != null) {
            Component.translatable("gui.appliedflooring.laser_connector.target_connected") to 0x00E676
        } else {
            val level = minecraft?.level
            val checkPos = be.blockPos.above(be.targetYOffset.coerceAtLeast(0))
            val st = level?.getBlockState(checkPos)
            if (be.targetYOffset == 0) {
                Component.translatable("gui.appliedflooring.laser_connector.target_self") to 0x00D2FF
            } else if (st == null || st.isAir || st.material.isReplaceable) {
                Component.translatable("gui.appliedflooring.laser_connector.target_available") to 0xFFB300
            } else {
                Component.translatable("gui.appliedflooring.laser_connector.target_blocked") to 0xFF5252
            }
        }
        font.draw(poseStack, targetStatusText, 104.0f, 42.0f, targetStatusColor)

        val radiusText = Component.translatable("gui.appliedflooring.laser_connector.radius", be.radiusX, be.radiusX * 2 + 1, be.radiusX * 2 + 1)
        font.draw(poseStack, radiusText, 32.0f, 108.0f, 0xE0E6ED)

        val statusDesc = Component.translatable("gui.appliedflooring.laser_connector.status_label", be.builderStatus.getDisplayName())
        val statusColor = when (be.builderStatus) {
            BuilderStatus.BUILDING -> 0x00D2FF
            BuilderStatus.DONE -> 0x00E676
            BuilderStatus.PAUSED -> 0xFFB300
            BuilderStatus.NO_POWER, BuilderStatus.NO_ITEMS, BuilderStatus.BLOCKED -> 0xFF5252
            BuilderStatus.IDLE -> 0x8C9BAE
        }
        font.draw(poseStack, statusDesc, 14.0f, 138.0f, statusColor)

        val percent = if (be.totalBlocksCount > 0) ((be.placedBlocksCount.toDouble() / be.totalBlocksCount) * 100).toInt() else 0
        val countText = Component.translatable("gui.appliedflooring.laser_connector.progress", be.placedBlocksCount, be.totalBlocksCount, percent)
        font.draw(poseStack, countText, 14.0f, 152.0f, 0xE0E6ED)
    }
}
