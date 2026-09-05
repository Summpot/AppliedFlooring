package io.github.summpot.appliedflooring.client.dynamic

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object VirtualTextureSynthesizer {

    private val COLOR_DELTAS: Map<String, Triple<Int, Int, Int>> = mapOf(
        "" to Triple(0, 0, 0),
        "black_" to Triple(-36, -28, -45),
        "blue_" to Triple(-20, 40, 133),
        "brown_" to Triple(44, 10, -47),
        "cyan_" to Triple(-34, 96, 83),
        "gray_" to Triple(6, 14, -1),
        "green_" to Triple(-30, 66, -17),
        "light_blue_" to Triple(4, 106, 150),
        "light_gray_" to Triple(74, 83, 65),
        "lime_" to Triple(30, 124, -43),
        "magenta_" to Triple(120, 10, 67),
        "orange_" to Triple(156, 53, -49),
        "pink_" to Triple(162, 66, 83),
        "purple_" to Triple(54, 0, 93),
        "red_" to Triple(117, -17, -33),
        "white_" to Triple(144, 154, 135),
        "yellow_" to Triple(172, 133, -37)
    )

    // In-memory cache mapping relative resource paths (e.g. "textures/block/fusion/blue_me_flooring_online.png") to byte content
    val RESOURCES = ConcurrentHashMap<String, ByteArray>()
    val RESOURCE_PATHS: Set<String> get() {
        ensureInitialized()
        return RESOURCES.keys
    }

    @Volatile
    private var initialized = false

    @Synchronized
    fun ensureInitialized() {
        if (initialized) return
        try {
            synthesizeAll()
            initialized = true
        } catch (e: Exception) {
            System.err.println("[AppliedFlooring] Failed to synthesize dynamic textures: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadMasterImage(filename: String): BufferedImage {
        val stream: InputStream = javaClass.getResourceAsStream("/assets/appliedflooring/textures/block/$filename")
            ?: throw IllegalStateException("Master texture /assets/appliedflooring/textures/block/$filename not found!")
        return stream.use { ImageIO.read(it) }
    }

    private fun synthesizeAll() {
        val states = listOf(
            "" to "online",
            "_offline" to "offline"
        )

        for ((suffix, stateName) in states) {
            val fMasterImg = loadMasterImage("me_flooring$suffix.png")
            val eMasterImg = loadMasterImage("me_elevator$suffix.png")
            val lMasterImg = loadMasterImage("me_laser_connector$suffix.png")

            val fMaster = toIntMatrix(fMasterImg)
            val eMaster = toIntMatrix(eMasterImg)
            val lMaster = toIntMatrix(lMasterImg)

            // Extract elevator and laser center icon masks (inside 3..12)
            val eIconMask = Array(16) { BooleanArray(16) }
            val lIconMask = Array(16) { BooleanArray(16) }
            for (y in 3..12) {
                for (x in 3..12) {
                    if (eMaster[y][x] != fMaster[y][x]) eIconMask[y][x] = true
                    if (lMaster[y][x] != fMaster[y][x]) lIconMask[y][x] = true
                }
            }

            // Generate 4 CTM slice matrices for flooring base
            val (fEmpty, fCenter, fH, fV) = generateFlooringSlices(fMaster)

            for ((colorPrefix, deltas) in COLOR_DELTAS) {
                val (dr, dg, db) = deltas

                // 1. Flooring slices with color delta applied
                val fBase = applyColorDelta(fMaster, dr, dg, db)
                val emptyF = applyColorDelta(fEmpty, dr, dg, db)
                val centerF = applyColorDelta(fCenter, dr, dg, db)
                val hF = applyColorDelta(fH, dr, dg, db)
                val vF = applyColorDelta(fV, dr, dg, db)

                // 2. Elevator slices (flooring base + preserved icon pixels)
                val eBase = overlayIcon(fBase, eMaster, eIconMask)
                val emptyE = overlayIcon(emptyF, eMaster, eIconMask)
                val centerE = overlayIcon(centerF, eMaster, eIconMask)
                val hE = overlayIcon(hF, eMaster, eIconMask)
                val vE = overlayIcon(vF, eMaster, eIconMask)

                // 3. Laser connector slices (flooring base + preserved lens pixels)
                val lBase = overlayIcon(fBase, lMaster, lIconMask)
                val emptyL = overlayIcon(emptyF, lMaster, lIconMask)
                val centerL = overlayIcon(centerF, lMaster, lIconMask)
                val hL = overlayIcon(hF, lMaster, lIconMask)
                val vL = overlayIcon(vF, lMaster, lIconMask)

                val blockTypes = listOf(
                    "${colorPrefix}me_flooring" to listOf(fBase, emptyF, centerF, hF, vF),
                    "${colorPrefix}me_elevator" to listOf(eBase, emptyE, centerE, hE, vE),
                    "${colorPrefix}me_laser_connector" to listOf(lBase, emptyL, centerL, hL, vL)
                )

                for ((blockName, slices) in blockTypes) {
                    val (base, empty, center, hSlice, vSlice) = slices

                    // Only generate dyed base textures (master uncolored 6 already exist on disk)
                    if (colorPrefix.isNotEmpty()) {
                        RESOURCES["textures/block/$blockName$suffix.png"] = toPngBytes(base)
                    }

                    // Athena 16x16 slices (only generated for online state)
                    if (stateName == "online") {
                        RESOURCES["textures/block/${blockName}_empty.png"] = toPngBytes(empty)
                        RESOURCES["textures/block/${blockName}_center.png"] = toPngBytes(center)
                        RESOURCES["textures/block/${blockName}_h.png"] = toPngBytes(hSlice)
                        RESOURCES["textures/block/${blockName}_v.png"] = toPngBytes(vSlice)
                    }

                    // Fusion 80x16 pieced texture
                    val fusionImg = buildPiecedImage(base, empty, vSlice, hSlice, center)
                    RESOURCES["textures/block/fusion/${blockName}_$stateName.png"] = toPngBytes(fusionImg)

                    // Fusion .mcmeta JSON file
                    val mcmetaJson = """{
  "fusion": {
    "type": "connecting",
    "layout": "pieced",
    "connections": {
      "type": "match_block",
      "blocks": [
        "appliedflooring:${colorPrefix}me_flooring",
        "appliedflooring:${colorPrefix}me_elevator",
        "appliedflooring:${colorPrefix}me_laser_connector"
      ]
    }
  }
}"""
                    RESOURCES["textures/block/fusion/${blockName}_$stateName.png.mcmeta"] =
                        mcmetaJson.toByteArray(Charsets.UTF_8)
                }
            }
        }
    }

    private fun toIntMatrix(img: BufferedImage): Array<IntArray> {
        val matrix = Array(16) { IntArray(16) }
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                matrix[y][x] = img.getRGB(x, y)
            }
        }
        return matrix
    }

    private fun toPngBytes(matrix: Array<IntArray>): ByteArray {
        val h = matrix.size
        val w = matrix[0].size
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until h) {
            for (x in 0 until w) {
                img.setRGB(x, y, matrix[y][x])
            }
        }
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "PNG", baos)
        return baos.toByteArray()
    }

    private fun overlayIcon(
        base: Array<IntArray>,
        masterIcon: Array<IntArray>,
        mask: Array<BooleanArray>
    ): Array<IntArray> {
        val out = Array(16) { y -> base[y].copyOf() }
        for (y in 3..12) {
            for (x in 3..12) {
                if (mask[y][x]) {
                    out[y][x] = masterIcon[y][x]
                }
            }
        }
        return out
    }

    private fun applyColorDelta(
        matrix: Array<IntArray>,
        dr: Int,
        dg: Int,
        db: Int
    ): Array<IntArray> {
        if (dr == 0 && dg == 0 && db == 0) {
            return Array(16) { y -> matrix[y].copyOf() }
        }
        val out = Array(16) { IntArray(16) }
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val argb = matrix[y][x]
                val a = (argb ushr 24) and 0xFF
                val r = ((argb ushr 16) and 0xFF) + dr
                val g = ((argb ushr 8) and 0xFF) + dg
                val b = (argb and 0xFF) + db
                val cr = r.coerceIn(0, 255)
                val cg = g.coerceIn(0, 255)
                val cb = b.coerceIn(0, 255)
                out[y][x] = (a shl 24) or (cr shl 16) or (cg shl 8) or cb
            }
        }
        return out
    }

    private fun buildPiecedImage(
        base: Array<IntArray>,
        empty: Array<IntArray>,
        vSlice: Array<IntArray>,
        hSlice: Array<IntArray>,
        center: Array<IntArray>
    ): Array<IntArray> {
        // 80x16 pieced layout: 5 slots of 16x16
        val out = Array(16) { IntArray(80) }
        val slots = listOf(base, empty, vSlice, hSlice, center)
        for (s in 0 until 5) {
            val slotMatrix = slots[s]
            val xOffset = s * 16
            for (y in 0 until 16) {
                for (x in 0 until 16) {
                    out[y][xOffset + x] = slotMatrix[y][x]
                }
            }
        }
        return out
    }

    private fun generateFlooringSlices(baseArr: Array<IntArray>): List<Array<IntArray>> {
        // Extract R, G, B channels as DoubleArray
        val rArr = DoubleArray(256)
        val gArr = DoubleArray(256)
        val bArr = DoubleArray(256)
        val knownMask = BooleanArray(256)

        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val idx = y * 16 + x
                val argb = baseArr[y][x]
                rArr[idx] = ((argb ushr 16) and 0xFF).toDouble()
                gArr[idx] = ((argb ushr 8) and 0xFF).toDouble()
                bArr[idx] = (argb and 0xFF).toDouble()
                if (y in 3..12 && x in 3..12) {
                    knownMask[idx] = true
                }
            }
        }

        // Laplace equation solver on periodic 16x16 torus
        val smoothR = solveTorusLaplace(rArr, knownMask, iterations = 200)

        // Calculate plate std and mean deltas for G and B
        var sumR = 0.0
        var sumG = 0.0
        var sumB = 0.0
        var countPlate = 0
        for (y in 3..12) {
            for (x in 3..12) {
                val idx = y * 16 + x
                sumR += rArr[idx]
                sumG += gArr[idx]
                sumB += bArr[idx]
                countPlate++
            }
        }
        val meanPlateR = sumR / countPlate
        var sumSqR = 0.0
        for (y in 3..12) {
            for (x in 3..12) {
                val diff = rArr[y * 16 + x] - meanPlateR
                sumSqR += diff * diff
            }
        }
        val stdPlateR = Math.sqrt(sumSqR / countPlate)
        val dg = ((sumG / countPlate) - meanPlateR).roundToInt()
        val db = ((sumB / countPlate) - meanPlateR).roundToInt()

        val noise = periodicLowfreqNoise(targetStd = stdPlateR)

        val emptyArr = Array(16) { IntArray(16) }
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val idx = y * 16 + x
                val r = if (knownMask[idx]) {
                    smoothR[idx]
                } else {
                    smoothR[idx] + noise[idx]
                }
                val cr = r.roundToInt().coerceIn(0, 255)
                val cg = (cr + dg).coerceIn(0, 255)
                val cb = (cr + db).coerceIn(0, 255)
                emptyArr[y][x] = (0xFF shl 24) or (cr shl 16) or (cg shl 8) or cb
            }
        }

        // Horizontal slice (hArr): connects east/west, borders north/south
        val hArr = Array(16) { y -> emptyArr[y].copyOf() }
        for (x in 0 until 16) {
            hArr[0][x] = baseArr[0][x]
            hArr[1][x] = baseArr[1][x]
            hArr[14][x] = baseArr[14][x]
            hArr[15][x] = baseArr[15][x]
        }
        val hMean1 = averageArgb(baseArr[1][1], baseArr[1][14])
        hArr[1][0] = hMean1
        hArr[1][15] = hMean1

        val grooveTop = averageRegionArgb(baseArr, 2..2, 3..12)
        for (x in 0 until 16) hArr[2][x] = grooveTop

        val grooveBot = averageRegionArgb(baseArr, 13..13, 3..12)
        for (x in 0 until 16) hArr[13][x] = grooveBot

        val hMean14 = averageArgb(baseArr[14][1], baseArr[14][14])
        hArr[14][0] = hMean14
        hArr[14][15] = hMean14

        val hMean15 = averageArgb(baseArr[15][1], baseArr[15][15])
        hArr[15][0] = hMean15
        hArr[15][15] = hMean15

        // Vertical slice (vArr): connects north/south, borders east/west
        val vArr = Array(16) { y -> emptyArr[y].copyOf() }
        for (y in 0 until 16) {
            vArr[y][0] = baseArr[y][0]
            vArr[y][1] = baseArr[y][1]
            vArr[y][14] = baseArr[y][14]
            vArr[y][15] = baseArr[y][15]
        }
        val vMean0 = averageArgb(baseArr[0][0], baseArr[14][0])
        vArr[15][0] = vMean0

        val vMean1 = averageArgb(baseArr[1][1], baseArr[14][1])
        vArr[0][1] = vMean1
        vArr[15][1] = vMean1

        val grooveLeft = averageRegionArgb(baseArr, 3..12, 2..2)
        for (y in 0 until 16) vArr[y][2] = grooveLeft

        val grooveRight = averageRegionArgb(baseArr, 3..12, 13..13)
        for (y in 0 until 16) vArr[y][13] = grooveRight

        val vMean14 = averageArgb(baseArr[1][14], baseArr[14][14])
        vArr[0][14] = vMean14
        vArr[15][14] = vMean14

        val vMean15 = averageArgb(baseArr[1][15], baseArr[15][15])
        vArr[0][15] = vMean15
        vArr[15][15] = vMean15

        // Center slice: inner corner (only keeps 4 corner pixels)
        val centerArr = Array(16) { y -> emptyArr[y].copyOf() }
        centerArr[0][0] = baseArr[0][0]
        centerArr[0][15] = baseArr[0][15]
        centerArr[15][0] = baseArr[15][0]
        centerArr[15][15] = baseArr[15][15]

        return listOf(emptyArr, centerArr, hArr, vArr)
    }

    private fun solveTorusLaplace(channel: DoubleArray, knownMask: BooleanArray, iterations: Int): DoubleArray {
        val u = channel.copyOf()
        for (iter in 0 until iterations) {
            for (y in 0 until 16) {
                for (x in 0 until 16) {
                    val idx = y * 16 + x
                    if (!knownMask[idx]) {
                        val up = u[((y - 1 + 16) % 16) * 16 + x]
                        val down = u[((y + 1) % 16) * 16 + x]
                        val left = u[y * 16 + ((x - 1 + 16) % 16)]
                        val right = u[y * 16 + ((x + 1) % 16)]
                        u[idx] = 0.25 * (up + down + left + right)
                    }
                }
            }
        }
        return u
    }

    private fun periodicLowfreqNoise(targetStd: Double, seed: Long = 42L): DoubleArray {
        val rng = Random(seed)
        val base = Array(16) { DoubleArray(16) { rng.nextGaussian() } }

        // Simple 3x3 gaussian blur on 16x16 torus (sigma ~ 0.5)
        // Kernel weights: center = 0.5, orthogonal = 0.1, diagonal = 0.025
        val blurred = Array(16) { DoubleArray(16) }
        var sum = 0.0
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                var v = base[y][x] * 0.5
                v += (base[(y - 1 + 16) % 16][x] + base[(y + 1) % 16][x] + base[y][(x - 1 + 16) % 16] + base[y][(x + 1) % 16]) * 0.1
                v += (base[(y - 1 + 16) % 16][(x - 1 + 16) % 16] + base[(y - 1 + 16) % 16][(x + 1) % 16] +
                        base[(y + 1) % 16][(x - 1 + 16) % 16] + base[(y + 1) % 16][(x + 1) % 16]) * 0.025
                blurred[y][x] = v
                sum += v
            }
        }
        val mean = sum / 256.0
        var sumSq = 0.0
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                val d = blurred[y][x] - mean
                sumSq += d * d
            }
        }
        val std = Math.sqrt(sumSq / 256.0)
        val scale = if (std > 0.0) targetStd / std else 1.0

        val result = DoubleArray(256)
        for (y in 0 until 16) {
            for (x in 0 until 16) {
                result[y * 16 + x] = (blurred[y][x] - mean) * scale
            }
        }
        return result
    }

    private fun averageArgb(c1: Int, c2: Int): Int {
        val r = (((c1 ushr 16) and 0xFF) + ((c2 ushr 16) and 0xFF)) / 2
        val g = (((c1 ushr 8) and 0xFF) + ((c2 ushr 8) and 0xFF)) / 2
        val b = ((c1 and 0xFF) + (c2 and 0xFF)) / 2
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun averageRegionArgb(matrix: Array<IntArray>, yRange: IntRange, xRange: IntRange): Int {
        var sumR = 0
        var sumG = 0
        var sumB = 0
        var count = 0
        for (y in yRange) {
            for (x in xRange) {
                val c = matrix[y][x]
                sumR += (c ushr 16) and 0xFF
                sumG += (c ushr 8) and 0xFF
                sumB += c and 0xFF
                count++
            }
        }
        return if (count > 0) {
            (0xFF shl 24) or ((sumR / count) shl 16) or ((sumG / count) shl 8) or (sumB / count)
        } else {
            0xFF000000.toInt()
        }
    }
}
