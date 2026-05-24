package com.example.toolbox.function.mouse

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class AlwaysAliveLifecycle : LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    
    init {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
}

class AlwaysAliveSavedStateRegistryOwner : SavedStateRegistryOwner {
    private val controller = SavedStateRegistryController.create(this)
    
    init {
        controller.performRestore(null)
    }
    
    override val savedStateRegistry: SavedStateRegistry
        get() = controller.savedStateRegistry
}

class MouseOverlayManager(private val context: Context) {
    private var windowManager: WindowManager? = null
    private var mousePointerView: View? = null
    private var controlPanelView: View? = null
    private var minimizeButtonView: View? = null
    private var isRunning = false
    private var isMinimized = false
    
    private var mouseX = 0f
    private var mouseY = 0f
    
    private var lastUpdateX = 0
    private var lastUpdateY = 0
    
    private val appContext = context.applicationContext
    private val alwaysAliveLifecycle = AlwaysAliveLifecycle()
    private val alwaysAliveSavedStateRegistry = AlwaysAliveSavedStateRegistryOwner()
    
    init {
        windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(appContext)
        } else {
            true
        }
    }

    fun hasAccessibilityPermission(): Boolean {
        return MouseAccessibilityService.isServiceRunning()
    }

    fun startOverlay(mouseViewModel: MouseViewModel, onDismiss: () -> Unit) {
        if (!hasOverlayPermission()) {
            throw SecurityException("没有悬浮窗权限")
        }
        
        if (isRunning) return
        
        val displayMetrics = appContext.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        
        mouseX = screenWidth / 2f
        mouseY = screenHeight / 2f
        
        createMousePointer(mouseViewModel)
        createControlPanel(mouseViewModel, onDismiss, screenWidth, screenHeight)
        createMinimizeButton()
        minimizeButtonView?.visibility = View.GONE
        
        isRunning = true
        isMinimized = false
    }

    private fun createMousePointer(viewModel: MouseViewModel) {
        val composeView = ComposeView(appContext)
        composeView.setViewTreeLifecycleOwner(alwaysAliveLifecycle)
        composeView.setViewTreeSavedStateRegistryOwner(alwaysAliveSavedStateRegistry)
        
        composeView.setContent {
            MousePointerComposable(
                size = viewModel.mouseSize,
                alpha = viewModel.mouseAlpha / 100f,
                style = viewModel.mouseStyle,
                showClock = viewModel.showClock,
                showBattery = viewModel.showBattery
            )
        }
        
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = mouseX.toInt()
            y = mouseY.toInt()
        }
        
        mousePointerView = composeView
        windowManager?.addView(composeView, params)
    }

    private fun createControlPanel(
        viewModel: MouseViewModel,
        onDismiss: () -> Unit,
        screenWidth: Int,
        screenHeight: Int
    ) {
        val composeView = ComposeView(appContext)
        composeView.setViewTreeLifecycleOwner(alwaysAliveLifecycle)
        composeView.setViewTreeSavedStateRegistryOwner(alwaysAliveSavedStateRegistry)

        composeView.setContent {
            ControlPanelComposable(
                onMove = { dx, dy ->
                    mouseX += dx
                    mouseY += dy
                    
                    mouseX = mouseX.coerceIn(0f, screenWidth.toFloat())
                    mouseY = mouseY.coerceIn(0f, screenHeight.toFloat())
                    
                    updateMousePosition()
                },
                onClick = { performClick() },
                onLongClick = { performLongClick() },
                onSwipe = { direction ->
                    when (direction) {
                        SwipeDirection.UP -> performSwipe(0f, -200f)
                        SwipeDirection.DOWN -> performSwipe(0f, 200f)
                        SwipeDirection.LEFT -> performSwipe(-200f, 0f)
                        SwipeDirection.RIGHT -> performSwipe(200f, 0f)
                    }
                },
                onMinimize = {
                    toggleMinimize()
                },
                onClose = {
                    stopOverlay()
                    onDismiss()
                },
                speed = viewModel.mouseSpeed
            )
        }
        
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.TOP or Gravity.START
            x = screenWidth / 2 - 140
            y = screenHeight / 4
        }
        
        controlPanelView = composeView
        windowManager?.addView(composeView, params)
    }

    private fun toggleMinimize() {
        isMinimized = !isMinimized
        if (isMinimized) {
            controlPanelView?.visibility = View.GONE
            minimizeButtonView?.visibility = View.VISIBLE
        } else {
            controlPanelView?.visibility = View.VISIBLE
            minimizeButtonView?.visibility = View.GONE
        }
    }

    private fun createMinimizeButton() {
        if (minimizeButtonView != null) return
        
        val composeView = ComposeView(appContext)
        composeView.setViewTreeLifecycleOwner(alwaysAliveLifecycle)
        composeView.setViewTreeSavedStateRegistryOwner(alwaysAliveSavedStateRegistry)
        
        composeView.setContent {
            MinimizeButtonComposable(
                onRestore = {
                    toggleMinimize()
                }
            )
        }
        
        val params = WindowManager.LayoutParams().apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            format = PixelFormat.TRANSLUCENT
            gravity = Gravity.BOTTOM or Gravity.END
            x = 50
            y = 200
        }
        
        minimizeButtonView = composeView
        windowManager?.addView(composeView, params)
    }

    private fun updateMousePosition() {
        val newX = mouseX.toInt()
        val newY = mouseY.toInt()
        
        if (newX != lastUpdateX || newY != lastUpdateY) {
            lastUpdateX = newX
            lastUpdateY = newY
            mousePointerView?.let { view ->
                val params = view.layoutParams as WindowManager.LayoutParams
                params.x = newX
                params.y = newY
                windowManager?.updateViewLayout(view, params)
            }
        }
    }

    private fun performClick() {
        if (MouseAccessibilityService.isServiceRunning()) {
            MouseAccessibilityService.instance?.performClick(mouseX, mouseY) {
                android.util.Log.d("MouseOverlay", "Click executed at ($mouseX, $mouseY)")
            }
        } else {
            android.util.Log.w("MouseOverlay", "Accessibility service not running")
        }
    }

    private fun performLongClick() {
        if (MouseAccessibilityService.isServiceRunning()) {
            MouseAccessibilityService.instance?.performLongClick(mouseX, mouseY) {
                android.util.Log.d("MouseOverlay", "Long click executed at ($mouseX, $mouseY)")
            }
        } else {
            android.util.Log.w("MouseOverlay", "Accessibility service not running")
        }
    }

    private fun performSwipe(dx: Float, dy: Float) {
        if (MouseAccessibilityService.isServiceRunning()) {
            val toX = mouseX + dx
            val toY = mouseY + dy
            MouseAccessibilityService.instance?.performSwipe(mouseX, mouseY, toX, toY, 300) {
                android.util.Log.d("MouseOverlay", "Swipe executed from ($mouseX, $mouseY) to ($toX, $toY)")
            }
        } else {
            android.util.Log.w("MouseOverlay", "Accessibility service not running")
        }
    }

    fun stopOverlay() {
        if (!isRunning) return
        
        try {
            mousePointerView?.let {
                windowManager?.removeView(it)
            }
            controlPanelView?.let {
                windowManager?.removeView(it)
            }
            minimizeButtonView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        mousePointerView = null
        controlPanelView = null
        minimizeButtonView = null
        isRunning = false
        isMinimized = false
    }

    fun checkIsRunning(): Boolean = isRunning
}

enum class SwipeDirection {
    UP, DOWN, LEFT, RIGHT
}