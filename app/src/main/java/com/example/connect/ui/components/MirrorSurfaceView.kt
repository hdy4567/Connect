package com.example.connect.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * MirrorSurfaceView provides a raw rendering surface for 60fps video.
 * It bypasses view-layer overhead to ensure < 15ms display latency.
 */
class MirrorSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    interface Listener {
        fun onSurfaceAvailable(surface: android.view.Surface)
        fun onSurfaceDestroyed()
    }

    private var listener: Listener? = null

    init {
        holder.addCallback(this)
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val canvas: Canvas? = holder.lockCanvas()
        canvas?.drawColor(Color.BLACK)
        holder.unlockCanvasAndPost(canvas)
        listener?.onSurfaceAvailable(holder.surface)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        listener?.onSurfaceDestroyed()
    }
}
