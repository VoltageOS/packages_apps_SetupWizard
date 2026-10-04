/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.settings

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.SystemProperties
import android.os.UserHandle
import android.provider.Settings
import android.view.View
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON_OVERLAY
import android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.materialswitch.MaterialSwitch
import org.lineageos.setupwizard.NAVIGATION_OPTION_KEY
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.SetupWizardApp
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils
import org.lineageos.setupwizard.util.updateCheckedIcons

class NavigationSettingsActivity : BaseSetupWizardActivity() {

    private var selection: String = NAV_BAR_MODE_GESTURAL_OVERLAY

    private lateinit var hideGesturalHint: MaterialSwitch
    private lateinit var hideGesturalHintCard: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navBarEnabled = hasNavigationBar(this)

        setDescriptionText(getString(R.string.navigation_summary))

        val modeGroup = findViewById<MaterialButtonToggleGroup>(R.id.navigation_mode_group)
        val gestureButton = findViewById<MaterialButton>(R.id.mode_gesture)
        val swKeysButton = findViewById<MaterialButton>(R.id.mode_sw_keys)
        val navigationIllustration = findViewById<LottieAnimationView>(R.id.navigation_illustration)
        hideGesturalHint = findViewById(R.id.hide_navigation_hint)
        hideGesturalHintCard = findViewById(R.id.hide_navigation_hint_card)

        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }
            when (checkedId) {
                R.id.mode_gesture -> {
                    selection = NAV_BAR_MODE_GESTURAL_OVERLAY
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_fully_gestural)
                    setHintRevealed(revealed = true)
                }

                R.id.mode_sw_keys -> {
                    selection = NAV_BAR_MODE_3BUTTON_OVERLAY
                    navigationIllustration.setAnimation(R.raw.lottie_system_nav_3_button)
                    setHintRevealed(revealed = false)
                }
            }
            modeGroup.updateCheckedIcons(R.drawable.ic_check)
            navigationIllustration.playAnimation()
        }

        var available = 3
        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_GESTURAL_OVERLAY)) {
            gestureButton.visibility = View.GONE
            modeGroup.check(R.id.mode_sw_keys)
            available--
        }

        if (!SetupWizardUtils.isPackageInstalled(this, NAV_BAR_MODE_3BUTTON_OVERLAY)) {
            swKeysButton.visibility = View.GONE
            available--
        }

        if (!navBarEnabled || available <= 1) {
            SetupWizardApp.settingsBundle.putString(
                NAVIGATION_OPTION_KEY,
                NAV_BAR_MODE_3BUTTON_OVERLAY,
            )
            finishAction(RESULT_OK)
        }

        modeGroup.updateCheckedIcons(R.drawable.ic_check)

        setHintRevealed(selection == NAV_BAR_MODE_GESTURAL_OVERLAY)
    }

    private fun setHintRevealed(revealed: Boolean) {
        hideGesturalHintCard.visibility = if (revealed) View.VISIBLE else View.GONE
    }

    override fun onNextPressed() {
        SetupWizardApp.settingsBundle.putString(NAVIGATION_OPTION_KEY, selection)
        val hideHint = hideGesturalHint.isChecked
        Settings.System.putIntForUser(
            contentResolver,
            Settings.System.NAVIGATION_BAR_HINT,
            if (hideHint) 0 else 1,
            UserHandle.USER_CURRENT,
        )
        super.onNextPressed()
    }

    override val layoutResId = R.layout.setup_navigation

    override val titleResId = R.string.setup_navigation

    override val iconResId = R.drawable.ic_navigation

    companion object {
        fun hasNavigationBar(context: Context): Boolean {
            val resources: Resources = context.resources
            val resIdShow = resources.getIdentifier("config_showNavigationBar", "bool", "android")
            var hasNavigationBar = false
            if (resIdShow > 0) {
                hasNavigationBar = resources.getBoolean(resIdShow)
            }
            return hasNavigationBar || "1" == SystemProperties.get("qemu.hw.mainkeys")
        }
    }
}
