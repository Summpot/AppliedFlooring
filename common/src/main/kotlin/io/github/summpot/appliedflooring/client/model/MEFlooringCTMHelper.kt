package io.github.summpot.appliedflooring.client.model

import appeng.api.implementations.blockentities.IColorableBlockEntity
import appeng.api.util.AEColor
import io.github.summpot.appliedflooring.AppliedFlooringMod
import io.github.summpot.appliedflooring.block.MEElevatorBlock
import io.github.summpot.appliedflooring.block.MEFlooringBlock
import io.github.summpot.appliedflooring.block.MELaserConnectorBlock
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.state.BlockState

object MEFlooringCTMHelper {
    var bakeryService: IFaceBakeryService? = null

    const val SLICE_BASE = 0
    const val SLICE_H = 1
    const val SLICE_V = 2
    const val SLICE_EMPTY = 3
    const val SLICE_CENTER = 4

    // quadCache[powerState 0..1][face 0..5][quadrant 0..3][sliceType 0..4]
    private val quadCache: Array<Array<Array<Array<BakedQuad?>>>> = Array(2) {
        Array(6) {
            Array(4) {
                arrayOfNulls<BakedQuad>(5)
            }
        }
    }

    // Overlays: [powerState 0..1][face 0..5]
    private val elevatorOverlay: Array<Array<BakedQuad?>> = Array(2) { arrayOfNulls(6) }
    private val laserOverlay: Array<Array<BakedQuad?>> = Array(2) { arrayOfNulls(6) }

    @Volatile
    private var initialized = false

    fun initSprites() {
        val service = bakeryService ?: return
        val atlas = Minecraft.getInstance().modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS)

        fun getSprite(path: String): TextureAtlasSprite {
            val id = ResourceLocation.tryParse("${AppliedFlooringMod.MOD_ID}:$path")!!
            return atlas.getSprite(id)
        }

        val sprites = arrayOf(
            // powerState 0 (offline)
            arrayOf(
                getSprite("block/me_flooring_offline"),
                getSprite("block/me_flooring_offline_h"),
                getSprite("block/me_flooring_offline_v"),
                getSprite("block/me_flooring_offline_empty"),
                getSprite("block/me_flooring_offline_center")
            ),
            // powerState 1 (online)
            arrayOf(
                getSprite("block/me_flooring"),
                getSprite("block/me_flooring_h"),
                getSprite("block/me_flooring_v"),
                getSprite("block/me_flooring_empty"),
                getSprite("block/me_flooring_center")
            )
        )

        val elevatorIcons = arrayOf(
            getSprite("block/me_elevator_offline_icon"),
            getSprite("block/me_elevator_icon")
        )

        val laserIcons = arrayOf(
            getSprite("block/me_laser_connector_offline_icon"),
            getSprite("block/me_laser_connector_icon")
        )

        for (powerState in 0..1) {
            for (face in Direction.values()) {
                val fIdx = face.ordinal
                for (quadrant in 0..3) {
                    val b = getQuadrantBounds(face, quadrant)
                    for (sliceType in 0..4) {
                        val sprite = sprites[powerState][sliceType]
                        quadCache[powerState][fIdx][quadrant][sliceType] = service.bakeQuad(
                            b.minX, b.minY, b.minZ,
                            b.maxX, b.maxY, b.maxZ,
                            b.u1, b.v1, b.u2, b.v2,
                            sprite, face, 0
                        )
                    }
                }

                // Overlay quads for Elevator and Laser Connector
                val ob = getOverlayBounds(face)
                elevatorOverlay[powerState][fIdx] = service.bakeQuad(
                    ob.minX, ob.minY, ob.minZ,
                    ob.maxX, ob.maxY, ob.maxZ,
                    ob.u1, ob.v1, ob.u2, ob.v2,
                    elevatorIcons[powerState], face, -1
                )
                laserOverlay[powerState][fIdx] = service.bakeQuad(
                    ob.minX, ob.minY, ob.minZ,
                    ob.maxX, ob.maxY, ob.maxZ,
                    ob.u1, ob.v1, ob.u2, ob.v2,
                    laserIcons[powerState], face, -1
                )
            }
        }

        initialized = true
    }

    fun isInitialized(): Boolean = initialized

    fun computeConnections(level: BlockGetter, pos: BlockPos, state: BlockState): IntArray {
        val block = state.block as? MEFlooringBlock ?: return IntArray(6)
        val be = level.getBlockEntity(pos)
        val myColor = (be as? IColorableBlockEntity)?.color ?: block.color
        val myPowered = state.hasProperty(MEFlooringBlock.POWERED) && state.getValue(MEFlooringBlock.POWERED)

        val masks = IntArray(6)
        for (face in Direction.values()) {
            val fIdx = face.ordinal
            val hDir = getHDir(face)
            val vDir = getVDir(face)

            var faceMask = 0
            for (quadrant in 0..3) {
                val qH = if (quadrant == 0 || quadrant == 2) hDir.opposite else hDir
                val qV = if (quadrant == 0 || quadrant == 1) vDir.opposite else vDir

                val hConn = canConnect(level, pos.relative(qH), myColor, myPowered)
                val vConn = canConnect(level, pos.relative(qV), myColor, myPowered)

                val sliceType = when {
                    hConn && vConn -> {
                        val dConn = canConnect(level, pos.relative(qH).relative(qV), myColor, myPowered)
                        if (dConn) SLICE_EMPTY else SLICE_CENTER
                    }
                    hConn -> SLICE_H
                    vConn -> SLICE_V
                    else -> SLICE_BASE
                }

                faceMask = faceMask or (sliceType shl (quadrant * 3))
            }
            masks[fIdx] = faceMask
        }
        return masks
    }

    private fun canConnect(level: BlockGetter, neighborPos: BlockPos, myColor: AEColor, myPowered: Boolean): Boolean {
        val nState = level.getBlockState(neighborPos)
        val nBlock = nState.block as? MEFlooringBlock ?: return false
        if (nState.hasProperty(MEFlooringBlock.POWERED) && nState.getValue(MEFlooringBlock.POWERED) != myPowered) {
            return false
        }
        val nBe = level.getBlockEntity(neighborPos)
        val nColor = (nBe as? IColorableBlockEntity)?.color ?: nBlock.color
        return nColor == myColor
    }

    fun getQuads(
        state: BlockState,
        facing: Direction,
        connectionData: IntArray?,
        block: MEFlooringBlock
    ): List<BakedQuad> {
        val powerState = if (state.hasProperty(MEFlooringBlock.POWERED) && state.getValue(MEFlooringBlock.POWERED)) 1 else 0
        val fIdx = facing.ordinal
        val quads = ArrayList<BakedQuad>(5)

        if (connectionData != null) {
            val mask = connectionData[fIdx]
            val s0 = mask and 0x7
            val s1 = (mask shr 3) and 0x7
            val s2 = (mask shr 6) and 0x7
            val s3 = (mask shr 9) and 0x7

            quadCache[powerState][fIdx][0][s0]?.let { quads.add(it) }
            quadCache[powerState][fIdx][1][s1]?.let { quads.add(it) }
            quadCache[powerState][fIdx][2][s2]?.let { quads.add(it) }
            quadCache[powerState][fIdx][3][s3]?.let { quads.add(it) }
        } else {
            quadCache[powerState][fIdx][0][0]?.let { quads.add(it) }
            quadCache[powerState][fIdx][1][0]?.let { quads.add(it) }
            quadCache[powerState][fIdx][2][0]?.let { quads.add(it) }
            quadCache[powerState][fIdx][3][0]?.let { quads.add(it) }
        }

        if (block is MEElevatorBlock) {
            elevatorOverlay[powerState][fIdx]?.let { quads.add(it) }
        } else if (block is MELaserConnectorBlock) {
            laserOverlay[powerState][fIdx]?.let { quads.add(it) }
        }

        return quads
    }

    private fun getHDir(face: Direction): Direction {
        return when (face) {
            Direction.UP -> Direction.EAST
            Direction.DOWN -> Direction.EAST
            Direction.NORTH -> Direction.WEST
            Direction.SOUTH -> Direction.EAST
            Direction.WEST -> Direction.SOUTH
            Direction.EAST -> Direction.NORTH
        }
    }

    private fun getVDir(face: Direction): Direction {
        return when (face) {
            Direction.UP -> Direction.SOUTH
            Direction.DOWN -> Direction.SOUTH
            Direction.NORTH -> Direction.DOWN
            Direction.SOUTH -> Direction.DOWN
            Direction.WEST -> Direction.DOWN
            Direction.EAST -> Direction.DOWN
        }
    }

    data class QuadBounds(
        val minX: Float, val minY: Float, val minZ: Float,
        val maxX: Float, val maxY: Float, val maxZ: Float,
        val u1: Float, val v1: Float,
        val u2: Float, val v2: Float
    )

    private fun getQuadrantBounds(face: Direction, quadrant: Int): QuadBounds {
        val u1 = if (quadrant == 0 || quadrant == 2) 0f else 8f
        val u2 = if (quadrant == 0 || quadrant == 2) 8f else 16f
        val v1 = if (quadrant == 0 || quadrant == 1) 0f else 8f
        val v2 = if (quadrant == 0 || quadrant == 1) 8f else 16f

        return when (face) {
            Direction.UP -> {
                val x1 = if (quadrant == 0 || quadrant == 2) 0f else 8f
                val x2 = if (quadrant == 0 || quadrant == 2) 8f else 16f
                val z1 = if (quadrant == 0 || quadrant == 1) 0f else 8f
                val z2 = if (quadrant == 0 || quadrant == 1) 8f else 16f
                QuadBounds(x1, 0f, z1, x2, 16f, z2, u1, v1, u2, v2)
            }
            Direction.DOWN -> {
                val x1 = if (quadrant == 0 || quadrant == 2) 0f else 8f
                val x2 = if (quadrant == 0 || quadrant == 2) 8f else 16f
                val z1 = if (quadrant == 0 || quadrant == 1) 0f else 8f
                val z2 = if (quadrant == 0 || quadrant == 1) 8f else 16f
                QuadBounds(x1, 0f, z1, x2, 16f, z2, u1, v1, u2, v2)
            }
            Direction.NORTH -> {
                val x1 = if (quadrant == 0 || quadrant == 2) 8f else 0f
                val x2 = if (quadrant == 0 || quadrant == 2) 16f else 8f
                val y1 = if (quadrant == 0 || quadrant == 1) 8f else 0f
                val y2 = if (quadrant == 0 || quadrant == 1) 16f else 8f
                QuadBounds(x1, y1, 0f, x2, y2, 16f, u1, v1, u2, v2)
            }
            Direction.SOUTH -> {
                val x1 = if (quadrant == 0 || quadrant == 2) 0f else 8f
                val x2 = if (quadrant == 0 || quadrant == 2) 8f else 16f
                val y1 = if (quadrant == 0 || quadrant == 1) 8f else 0f
                val y2 = if (quadrant == 0 || quadrant == 1) 16f else 8f
                QuadBounds(x1, y1, 0f, x2, y2, 16f, u1, v1, u2, v2)
            }
            Direction.WEST -> {
                val z1 = if (quadrant == 0 || quadrant == 2) 0f else 8f
                val z2 = if (quadrant == 0 || quadrant == 2) 8f else 16f
                val y1 = if (quadrant == 0 || quadrant == 1) 8f else 0f
                val y2 = if (quadrant == 0 || quadrant == 1) 16f else 8f
                QuadBounds(0f, y1, z1, 16f, y2, z2, u1, v1, u2, v2)
            }
            Direction.EAST -> {
                val z1 = if (quadrant == 0 || quadrant == 2) 8f else 0f
                val z2 = if (quadrant == 0 || quadrant == 2) 16f else 8f
                val y1 = if (quadrant == 0 || quadrant == 1) 8f else 0f
                val y2 = if (quadrant == 0 || quadrant == 1) 16f else 8f
                QuadBounds(0f, y1, z1, 16f, y2, z2, u1, v1, u2, v2)
            }
        }
    }

    private fun getOverlayBounds(face: Direction): QuadBounds {
        val u1 = 3f
        val v1 = 3f
        val u2 = 13f
        val v2 = 13f
        val delta = 0.01f

        return when (face) {
            Direction.UP -> QuadBounds(3f, 0f, 3f, 13f, 16f + delta, 13f, u1, v1, u2, v2)
            Direction.DOWN -> QuadBounds(3f, -delta, 3f, 13f, 16f, 13f, u1, v1, u2, v2)
            Direction.NORTH -> QuadBounds(3f, 3f, -delta, 13f, 13f, 16f, u1, v1, u2, v2)
            Direction.SOUTH -> QuadBounds(3f, 3f, 0f, 13f, 13f, 16f + delta, u1, v1, u2, v2)
            Direction.WEST -> QuadBounds(-delta, 3f, 3f, 16f, 13f, 13f, u1, v1, u2, v2)
            Direction.EAST -> QuadBounds(0f, 3f, 3f, 16f + delta, 13f, 13f, u1, v1, u2, v2)
        }
    }
}
