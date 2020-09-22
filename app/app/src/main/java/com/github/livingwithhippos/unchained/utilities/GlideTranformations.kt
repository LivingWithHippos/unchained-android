package com.github.livingwithhippos.unchained.utilities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RSRuntimeException
import android.renderscript.RenderScript
import android.renderscript.RenderScript.RSMessageHandler
import android.renderscript.ScriptIntrinsicBlur
import androidx.annotation.ColorInt
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest


private const val DEFAULT_DOWN_SAMPLING = 0.5f
private const val DEFAULT_ALPHA = 90

class BlurTransformation(private val context: Context) : BitmapTransformation() {

    private val id: String =
        "com.github.livingwithhippos.unchained.utilities.BlurTransformation:$context"
    private val idBytes: ByteArray = id.toByteArray()


    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap? {
        val source: Bitmap = toTransform
        val scaledWidth = (source.width * DEFAULT_DOWN_SAMPLING).toInt()
        val scaledHeight = (source.height * DEFAULT_DOWN_SAMPLING).toInt()
        val bitmap = pool[scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888]
        return BitmapResource.obtain(
            this.blurBitmap(
                context, source, bitmap, Color.argb(
                    DEFAULT_ALPHA,
                    255,
                    255,
                    255
                )
            ), pool
        )?.get()
    }

    override fun equals(other: Any?): Boolean {
        if (other is BlurTransformation)
            return context == other.context

        return false
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(idBytes)
    }

    @Synchronized
    fun blurBitmap(
        context: Context,
        source: Bitmap?,
        bitmap: Bitmap,
        @ColorInt colorOverlay: Int
    ): Bitmap? {
        if (source == null) return bitmap
        Canvas(bitmap).apply {
            scale(DEFAULT_DOWN_SAMPLING, DEFAULT_DOWN_SAMPLING)
            drawBitmap(source, 0f, 0f, Paint().apply {
                flags = Paint.FILTER_BITMAP_FLAG
            })
            drawColor(colorOverlay)
        }
        return try {
            blur(context, bitmap)
        } catch (e: RSRuntimeException) {
            e.printStackTrace()
            bitmap
        }
    }

    @Throws(RSRuntimeException::class)
    private fun blur(context: Context, bitmap: Bitmap): Bitmap {
        var rs: RenderScript? = null
        var input: Allocation? = null
        var output: Allocation? = null
        var blur: ScriptIntrinsicBlur? = null
        try {
            rs = RenderScript.create(context)
            rs.messageHandler = RSMessageHandler()
            input = Allocation.createFromBitmap(
                rs,
                bitmap,
                Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_SCRIPT
            )
            output = Allocation.createTyped(rs, input.type)
            blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)).apply {
                setInput(input)
                setRadius(25f)
                forEach(output)
            }
            output.copyTo(bitmap)
        } finally {
            rs?.destroy()
            input?.destroy()
            output?.destroy()
            blur?.destroy()
        }
        return bitmap
    }
}