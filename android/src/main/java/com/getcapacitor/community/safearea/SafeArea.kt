package com.getcapacitor.community.safearea

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class SafeArea(private val activity: Activity, private val webView: WebView) {
    var offset = 0
    private var appearanceUpdatedInListener = false
    private var decorFitsSystemWindowsNegated = false

    fun enable(updateInsets: Boolean, appearanceConfig: AppearanceConfig) {
        offset = appearanceConfig.offset
        
        activity.window.decorView.getRootView().setOnApplyWindowInsetsListener { view, insets ->
            updateInsets()
            if (!appearanceUpdatedInListener) {
                // @TODO: appearance is sometimes not updated on app load
                // probably because it is superseded by another plugin or native thing that updates the appearance
                // This is probably not the best way to override that behaviour
                // So we should think of something better than simply calling `updateAppearance` here
                updateAppearance(appearanceConfig)
                // Only update it once, to prevent an infinite loop
                appearanceUpdatedInListener = true
            }
            view.onApplyWindowInsets(insets)
        }

        resetDecorFitsSystemWindows()
        updateAppearance(appearanceConfig)

        if (updateInsets) {
            updateInsets()
        }
    }

    fun disable(appearanceConfig: AppearanceConfig) {
        activity.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
        }
        activity.window.decorView.getRootView().setOnApplyWindowInsetsListener(null)
        activity.window.decorView.setPadding(0, 0, 0, 0)

        updateAppearance(appearanceConfig)
        resetProperties()
    }

    fun resetDecorFitsSystemWindows() {
        decorFitsSystemWindowsNegated = false
    }

    private fun updateAppearance(appearanceConfig: AppearanceConfig) {
        activity.runOnUiThread {
            val windowInsetsControllerCompat =
                WindowCompat.getInsetsController(activity.window, activity.window.decorView)
            windowInsetsControllerCompat.isAppearanceLightStatusBars =
                appearanceConfig.statusBarContent == "dark"
            windowInsetsControllerCompat.isAppearanceLightNavigationBars =
                appearanceConfig.navigationBarContent == "dark"

            val window = activity.window

            if (appearanceConfig.customColorsForSystemBars) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                    window.decorView.setBackgroundColor(Color.parseColor(appearanceConfig.statusBarColor))
                } else {
                    window.statusBarColor = Color.parseColor(appearanceConfig.statusBarColor)
                    window.navigationBarColor = Color.parseColor(appearanceConfig.navigationBarColor)
                }
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }
        }
    }

    private fun updateInsets() {
        activity.runOnUiThread {
            val windowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            val systemBarsInsets =
                windowInsets?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: Insets.NONE
            val imeInsets = windowInsets?.getInsets(WindowInsetsCompat.Type.ime()) ?: Insets.NONE

            val density = activity.resources.displayMetrics.density

            val positionData = PositionData(
                top = Math.round(systemBarsInsets.top / density) + offset,
                bottom = if (imeInsets.bottom == 0) Math.round(systemBarsInsets.bottom / density) + offset else 0,
                left = Math.round(systemBarsInsets.left / density) + offset,
                right = Math.round(systemBarsInsets.right / density) + offset
            )

            // Set padding of decorview so the scroll view stays correct.
            // Otherwise the content behind the keyboard cannot be viewed by the user.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                if (!decorFitsSystemWindowsNegated) {
                    decorFitsSystemWindowsNegated = true
                    WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                }

                setPropertyEdge(positionData)
            } else {
                setProperty(positionData)
            }
        }
    }

    private fun resetProperties() {
        setProperty(
            PositionData(
                top = 0,
                bottom = 0,
                left = 0,
                right = 0
            )
        )
    }

    private fun setPropertyEdge(
        positionData: PositionData
    ) {
        activity.window.decorView.setPadding(positionData.left, positionData.top, positionData.right, positionData.bottom)
    }


    private fun setJsProperty(position: String, size: Int) {
        activity.runOnUiThread {
            webView.loadUrl(
                "javascript:document.querySelector(':root')?.style.setProperty('--safe-area-inset-" + position + "', 'max(env(safe-area-inset-" + position + "), " + size + "px)');void(0);")
        }
    }


    private fun setProperty(
        positionData: PositionData,
    ) {
        activity.window.decorView.setPadding(0, 0, 0, 0)
        setJsProperty("top", positionData.top)
        setJsProperty("bottom", positionData.bottom)
        setJsProperty("left", positionData.left)
        setJsProperty("right", positionData.right)
    }
}
