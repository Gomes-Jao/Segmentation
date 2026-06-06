package com.andre.tflite.classification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class SegmentationHelper(context: Context) {

    companion object {
        const val INPUT_SIZE = 128
        const val NUM_CLASSES = 2
        private const val MODEL_FILE = "model.tflite"
    }

    private val interpreter: Interpreter

    init {
        val model = loadModelFile(context, MODEL_FILE)
        interpreter = Interpreter(model)
    }

    fun segmentWithOverlay(source: Bitmap): Bitmap {
        val mask = runInference(source)
        return drawOverlay(source, mask)
    }

    private fun runInference(source: Bitmap): Array<IntArray> {
        val resized = Bitmap.createScaledBitmap(source, INPUT_SIZE, INPUT_SIZE, true)
        val inputShape = interpreter.getInputTensor(0).shape()
        val inputBuffer = bitmapToInputBuffer(resized, inputShape)

        val outputShape = interpreter.getOutputTensor(0).shape()
        val output = createOutputBuffer(outputShape)

        interpreter.run(inputBuffer, output)
        return logitsToMask(output, outputShape)
    }

    private fun createOutputBuffer(shape: IntArray): Array<Array<Array<FloatArray>>> {
        return when {
            shape.size == 4 && shape[1] == NUM_CLASSES ->
                Array(shape[0]) { Array(NUM_CLASSES) { Array(shape[2]) { FloatArray(shape[3]) } } }
            shape.size == 4 && shape[3] == NUM_CLASSES ->
                Array(shape[0]) { Array(shape[1]) { Array(shape[2]) { FloatArray(NUM_CLASSES) } } }
            else -> throw IllegalStateException("Formato de saída inesperado: ${shape.contentToString()}")
        }
    }

    private fun logitsToMask(output: Array<Array<Array<FloatArray>>>, shape: IntArray): Array<IntArray> {
        val mask = Array(INPUT_SIZE) { IntArray(INPUT_SIZE) }

        // NCHW: [1, C, H, W]
        if (shape.size == 4 && shape[1] == NUM_CLASSES) {
            for (y in 0 until INPUT_SIZE) {
                for (x in 0 until INPUT_SIZE) {
                    val bg = output[0][0][y][x]
                    val fg = output[0][1][y][x]
                    mask[y][x] = if (fg > bg) 1 else 0
                }
            }
            return mask
        }

        // NHWC: [1, H, W, C]
        if (shape.size == 4 && shape[3] == NUM_CLASSES) {
            for (y in 0 until INPUT_SIZE) {
                for (x in 0 until INPUT_SIZE) {
                    val bg = output[0][y][x][0]
                    val fg = output[0][y][x][1]
                    mask[y][x] = if (fg > bg) 1 else 0
                }
            }
            return mask
        }

        throw IllegalStateException("Formato de saída não suportado: ${shape.contentToString()}")
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap, shape: IntArray): ByteBuffer {
        val isNhwc = shape.size == 4 && shape[3] == 3
        val buffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        if (isNhwc) {
            for (pixel in pixels) {
                buffer.putFloat(((pixel shr 16) and 0xFF) / 255f)
                buffer.putFloat(((pixel shr 8) and 0xFF) / 255f)
                buffer.putFloat((pixel and 0xFF) / 255f)
            }
        } else {
            val planes = Array(3) { FloatArray(INPUT_SIZE * INPUT_SIZE) }
            for (i in pixels.indices) {
                planes[0][i] = ((pixels[i] shr 16) and 0xFF) / 255f
                planes[1][i] = ((pixels[i] shr 8) and 0xFF) / 255f
                planes[2][i] = (pixels[i] and 0xFF) / 255f
            }
            for (c in 0 until 3) {
                for (v in planes[c]) {
                    buffer.putFloat(v)
                }
            }
        }

        buffer.rewind()
        return buffer
    }

    private fun drawOverlay(source: Bitmap, mask: Array<IntArray>): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val scaleX = source.width.toFloat() / INPUT_SIZE
        val scaleY = source.height.toFloat() / INPUT_SIZE

        val canvas = Canvas(result)
        val paint = Paint().apply {
            color = Color.argb(115, 255, 64, 64)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                if (mask[y][x] == 1) {
                    val left = x * scaleX
                    val top = y * scaleY
                    canvas.drawRect(left, top, left + scaleX, top + scaleY, paint)
                }
            }
        }
        return result
    }

    private fun loadModelFile(context: Context, assetName: String): MappedByteBuffer {
        context.assets.openFd(assetName).use { asset ->
            FileInputStream(asset.fileDescriptor).use { stream ->
                return stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    asset.startOffset,
                    asset.declaredLength
                )
            }
        }
    }
}
