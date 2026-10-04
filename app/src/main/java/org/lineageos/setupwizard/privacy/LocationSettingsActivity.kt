/*
 * SPDX-FileCopyrightText: 2016 The CyanogenMod Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.privacy

import android.content.pm.PackageManager
import android.ext.settings.GeocoderSettings
import android.ext.settings.GnssSettings
import android.ext.settings.NetworkLocationSettings
import android.location.LocationManager
import android.os.Bundle
import android.os.Process
import android.os.UserManager
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.setupdesign.items.Item
import com.google.android.setupdesign.items.SwitchItem
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.base.BaseSetupWizardActivity

class LocationSettingsActivity : BaseSetupWizardActivity() {

    private val locationAccess by lazy {
        itemAdapter.findItemById(R.id.location_item) as SwitchItem
    }
    private val locationAgpsAccess by lazy {
        itemAdapter.findItemById(R.id.location_agps_item) as SwitchItem
    }
    private val suplItem by lazy {
        itemAdapter.findItemById(R.id.location_supl_item) as Item
    }
    private val psdsItem by lazy {
        itemAdapter.findItemById(R.id.location_psds_item) as Item
    }
    private val networkItem by lazy {
        itemAdapter.findItemById(R.id.location_network_item) as Item
    }
    private val geocoderItem by lazy {
        itemAdapter.findItemById(R.id.location_geocoder_item) as Item
    }

    private val locationManager by lazy { getSystemService(LocationManager::class.java) }
    private val userManager by lazy { getSystemService(UserManager::class.java) }

    private var suplPicker: ChoicePicker? = null
    private var psdsPicker: ChoicePicker? = null
    private var networkPicker: ChoicePicker? = null
    private var geocoderPicker: ChoicePicker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDescriptionText(getString(R.string.location_summary))

        val isMainUser = userManager.isMainUser
        locationAgpsAccess.isVisible = isMainUser

        val hasGpsFeature = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
        val suplAvailable = isMainUser && hasGpsFeature
        val psdsAvailable = suplAvailable &&
            getString(com.android.internal.R.string.config_gnssPsdsType).isNotEmpty()

        suplPicker = createPicker(
            suplAvailable,
            suplItem,
            R.string.location_supl_title,
            R.string.location_supl_desc,
            intArrayOf(
                GnssSettings.SUPL_SERVER_GRAPHENEOS_PROXY,
                GnssSettings.SUPL_SERVER_STANDARD,
                GnssSettings.SUPL_DISABLED,
            ),
            intArrayOf(
                R.string.location_supl_grapheneos_proxy,
                R.string.location_supl_standard_server,
                R.string.location_supl_off,
            ),
            GnssSettings.SUPL_SETTING.get(this),
            savedInstanceState,
            KEY_SUPL,
        )

        psdsPicker = createPicker(
            psdsAvailable,
            psdsItem,
            R.string.location_psds_title,
            R.string.location_psds_desc,
            intArrayOf(
                GnssSettings.PSDS_SERVER_GRAPHENEOS,
                GnssSettings.PSDS_SERVER_STANDARD,
                GnssSettings.PSDS_DISABLED,
            ),
            intArrayOf(
                R.string.location_psds_grapheneos_server,
                R.string.location_psds_standard_server,
                R.string.location_psds_off,
            ),
            GnssSettings.getPsdsSetting(this).get(this),
            savedInstanceState,
            KEY_PSDS,
        )

        networkPicker = createPicker(
            isMainUser,
            networkItem,
            R.string.location_network_title,
            R.string.location_network_desc,
            intArrayOf(
                NetworkLocationSettings.NETWORK_LOCATION_GRAPHENEOS_APPLE_PROXY,
                NetworkLocationSettings.NETWORK_LOCATION_APPLE,
                NetworkLocationSettings.NETWORK_LOCATION_APPLE_CHINA,
                NetworkLocationSettings.NETWORK_LOCATION_DISABLED,
            ),
            intArrayOf(
                R.string.location_network_grapheneos_apple_proxy,
                R.string.location_network_apple,
                R.string.location_network_apple_china,
                R.string.location_network_off,
            ),
            NetworkLocationSettings.NETWORK_LOCATION_SETTING.get(this),
            savedInstanceState,
            KEY_NETWORK_LOCATION,
        )

        geocoderPicker = createPicker(
            isMainUser,
            geocoderItem,
            R.string.location_geocoder_title,
            R.string.location_geocoder_desc,
            intArrayOf(
                GeocoderSettings.GEOCODER_SERVER_GRAPHENEOS,
                GeocoderSettings.GEOCODER_SERVER_OPENSTREETMAP,
                GeocoderSettings.GEOCODER_DISABLED,
            ),
            intArrayOf(
                R.string.location_geocoder_grapheneos_server,
                R.string.location_geocoder_openstreetmap,
                R.string.location_geocoder_off,
            ),
            GeocoderSettings.GEOCODER_SETTING.get(this),
            savedInstanceState,
            KEY_GEOCODER,
        )

        itemAdapter.setOnItemSelectedListener { item ->
            when (item) {
                is SwitchItem -> item.isChecked = !item.isChecked
                suplItem -> suplPicker?.show()
                psdsItem -> psdsPicker?.show()
                networkItem -> networkPicker?.show()
                geocoderItem -> geocoderPicker?.show()
            }
        }
    }

    private fun createPicker(
        available: Boolean,
        item: Item,
        titleResId: Int,
        descResId: Int,
        values: IntArray,
        labelResIds: IntArray,
        currentValue: Int,
        savedInstanceState: Bundle?,
        key: String,
    ): ChoicePicker? {
        if (!available) {
            item.isVisible = false
            return null
        }
        val value = if (savedInstanceState == null) {
            currentValue
        } else {
            savedInstanceState.getInt(key, currentValue)
        }
        return ChoicePicker(item, titleResId, descResId, values, labelResIds, value)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        suplPicker?.let { outState.putInt(KEY_SUPL, it.value) }
        psdsPicker?.let { outState.putInt(KEY_PSDS, it.value) }
        networkPicker?.let { outState.putInt(KEY_NETWORK_LOCATION, it.value) }
        geocoderPicker?.let { outState.putInt(KEY_GEOCODER, it.value) }
    }

    override fun onResume() {
        super.onResume()
        var checked = locationManager.isLocationEnabled
        if (userManager.isManagedProfile) {
            checked =
                checked and userManager.hasUserRestriction(UserManager.DISALLOW_SHARE_LOCATION)
        }
        locationAccess.isChecked = checked
    }

    override fun onNextPressed() {
        locationManager.setLocationEnabledForUser(locationAccess.isChecked, Process.myUserHandle())
        if (userManager.isManagedProfile) {
            userManager.setUserRestriction(
                UserManager.DISALLOW_SHARE_LOCATION,
                !locationAccess.isChecked,
            )
        }
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.ASSISTED_GPS_ENABLED,
            if (locationAgpsAccess.isChecked) 1 else 0,
        )
        suplPicker?.let { GnssSettings.SUPL_SETTING.put(this, it.value) }
        psdsPicker?.let { GnssSettings.getPsdsSetting(this).put(this, it.value) }
        networkPicker?.let {
            NetworkLocationSettings.NETWORK_LOCATION_SETTING.put(this, it.value)
        }
        geocoderPicker?.let { GeocoderSettings.GEOCODER_SETTING.put(this, it.value) }
        super.onNextPressed()
    }

    override val layoutResId = R.layout.location_settings

    override val titleResId = R.string.setup_location

    override val iconResId = R.drawable.ic_location

    private inner class ChoicePicker(
        private val item: Item,
        private val titleResId: Int,
        private val descResId: Int,
        private val values: IntArray,
        private val labelResIds: IntArray,
        var value: Int,
    ) {
        init {
            updateSummary()
        }

        private fun selectedIndex(): Int {
            for (i in values.indices) {
                if (values[i] == value) {
                    return i
                }
            }
            return 0
        }

        private fun updateSummary() {
            item.summary = getString(labelResIds[selectedIndex()])
        }

        fun show() {
            val context = this@LocationSettingsActivity
            val density = context.resources.displayMetrics.density
            val horizontal = (24 * density).toInt()
            val vertical = (8 * density).toInt()
            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(horizontal, vertical, horizontal, vertical)
            }
            container.addView(TextView(context).apply { setText(descResId) })
            val group = RadioGroup(context).apply {
                setPadding(0, vertical, 0, 0)
            }
            for (i in labelResIds.indices) {
                group.addView(MaterialRadioButton(context).apply {
                    id = View.generateViewId()
                    text = getString(labelResIds[i])
                    isChecked = values[i] == value
                })
            }
            container.addView(group)
            val dialog = MaterialAlertDialogBuilder(
                context,
                com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog,
            )
                .setTitle(titleResId)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .create()
            group.setOnCheckedChangeListener { _, checkedId ->
                val index = group.indexOfChild(group.findViewById(checkedId))
                if (index >= 0) {
                    value = values[index]
                    updateSummary()
                }
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    companion object {
        private const val KEY_SUPL = "supl"
        private const val KEY_PSDS = "psds"
        private const val KEY_NETWORK_LOCATION = "network_location"
        private const val KEY_GEOCODER = "geocoder"
    }
}
