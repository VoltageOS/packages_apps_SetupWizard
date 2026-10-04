/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.settings

import android.app.UiModeManager
import android.content.res.Configuration
import android.graphics.Outline
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import com.google.android.material.button.MaterialButtonToggleGroup
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.updateCheckedIcons

class ThemeSettingsActivity : BaseSetupWizardActivity() {

    private val uiModeManager by lazy { getSystemService(UiModeManager::class.java) }
    private lateinit var blackThemeCard: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setDescriptionText(getString(R.string.theme_summary))

        val modeGroup = findViewById<MaterialButtonToggleGroup>(R.id.theme_mode_group)
        blackThemeCard = findViewById(R.id.black_theme_card)
        blackThemeCard.visibility = View.GONE
        roundPreviewWallpaper(findViewById(R.id.theme_preview_wallpaper))
        val isNight =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        modeGroup.check(if (isNight) R.id.mode_dark else R.id.mode_light)
        modeGroup.updateCheckedIcons(R.drawable.ic_check)

        modeGroup.addOnButtonCheckedListener { _, checkedId, checked ->
            if (!checked) {
                return@addOnButtonCheckedListener
            }
            val night = checkedId == R.id.mode_dark
            modeGroup.updateCheckedIcons(R.drawable.ic_check)
            uiModeManager.setNightModeActivated(night)
        }
    }

    private fun roundPreviewWallpaper(wallpaper: ImageView) {
        wallpaper.outlineProvider =
            object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    val artwork = wallpaper.drawable ?: return
                    val scale =
                        minOf(
                            view.width.toFloat() / artwork.intrinsicWidth,
                            view.height.toFloat() / artwork.intrinsicHeight,
                        )
                    val width = (artwork.intrinsicWidth * scale).toInt()
                    val height = (artwork.intrinsicHeight * scale).toInt()
                    val left = (view.width - width) / 2
                    val top = (view.height - height) / 2
                    val radius = width * PREVIEW_CORNER_RADIUS / PREVIEW_WIDTH
                    outline.setRoundRect(
                        left,
                        top,
                        left + width,
                        top + height + radius.toInt(),
                        radius,
                    )
                }
            }
        wallpaper.clipToOutline = true
    }

    override val layoutResId = R.layout.setup_theme

    override val titleResId = R.string.setup_theme

    override val iconResId = R.drawable.ic_palette

    companion object {
        // Both taken from the preview artwork's viewport, see theme_preview_qs.xml.
        private const val PREVIEW_WIDTH = 412f
        private const val PREVIEW_CORNER_RADIUS = 28f
    }
}
