package com.getcapacitor.community.safearea

import android.app.Activity
import android.graphics.Color
import android.view.WindowManager
import android.webkit.WebView
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

class SafeArea(private val activity: Activity, private val webView: WebView) {
    var offset = 0
    private var appearanceUpdatedInListener = false
    private var decorFitsSystemWindowsNegated = false

    fun enable(updateInsets: Boolean, appearanceConfig: AppearanceConfig) {
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
                window.decorView.setBackgroundColor(Color.parseColor(appearanceConfig.backgroundColor))
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            }
        }
    }

    private fun updateInsets() {
        activity.runOnUiThread {
            if (!decorFitsSystemWindowsNegated) {
                decorFitsSystemWindowsNegated = true
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            }

            val windowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView)
            val systemBarsInsets =
                windowInsets?.getInsets(WindowInsetsCompat.Type.systemBars()) ?: Insets.NONE
            val imeInsets = windowInsets?.getInsets(WindowInsetsCompat.Type.ime()) ?: Insets.NONE

            val density = activity.resources.displayMetrics.density

            val topVal = Math.round(systemBarsInsets.top / density) + offset
            val leftVal =  Math.round(systemBarsInsets.left / density)
            val rightVal = Math.round(systemBarsInsets.right / density)
            var bottomVal = 0
            if (imeInsets.bottom == 0) {
                bottomVal = Math.round(systemBarsInsets.bottom / density) + offset
            }

            // To get the actual height of the keyboard, we need to subtract the height of the system bars from the height of the ime
            // Source: https://stackoverflow.com/a/75328335/8634342
            val imeHeight = (imeInsets.bottom - systemBarsInsets.bottom).coerceAtLeast(0)

            // Set padding of decorview so the scroll view stays correct.
            // Otherwise the content behind the keyboard cannot be viewed by the user.
            activity.window.decorView.setPadding(leftVal, topVal, rightVal, bottomVal + imeHeight)
        }
    }
}
