/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.setupwizard.system

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.setupcompat.util.ResultCodes.RESULT_SKIP
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.lineageos.setupwizard.R
import org.lineageos.setupwizard.base.BaseSetupWizardActivity
import org.lineageos.setupwizard.util.SetupWizardUtils

class AppStoreActivity : BaseSetupWizardActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private var installTask: ExecutorService? = null
    private lateinit var installButton: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var statusView: TextView
    private var receiverRegistered = false

    private val installResultReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handleInstallResult(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (shouldSkip()) {
            finishAction(RESULT_SKIP)
            return
        }
        installButton = findViewById(R.id.appstore_install_button)
        progressBar = findViewById(R.id.appstore_progress)
        statusView = findViewById(R.id.appstore_status)
        installButton.setOnClickListener { startInstall() }
        registerReceiver(
            installResultReceiver,
            IntentFilter(ACTION_INSTALL_RESULT),
            Context.RECEIVER_NOT_EXPORTED,
        )
        receiverRegistered = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (receiverRegistered) {
            unregisterReceiver(installResultReceiver)
            receiverRegistered = false
        }
        installTask?.shutdownNow()
        installTask = null
        handler.removeCallbacksAndMessages(null)
    }

    private fun shouldSkip(): Boolean {
        return !SetupWizardUtils.isOwner() ||
            SetupWizardUtils.isGmsCoreInstalled(this) ||
            SetupWizardUtils.isPackageInstalled(this, APPSTORE_PACKAGE) ||
            !SetupWizardUtils.isNetworkConnectedToInternet(this)
    }

    private fun startInstall() {
        installButton.isEnabled = false
        progressBar.visibility = View.VISIBLE
        progressBar.isIndeterminate = false
        progressBar.progress = 0
        statusView.setText(R.string.appstore_downloading)
        installTask?.shutdownNow()
        installTask = Executors.newSingleThreadExecutor()
        installTask?.execute {
            val apk = File(cacheDir, APK_FILE_NAME)
            try {
                download(apk)
                handler.post { onDownloadFinished() }
                install(apk)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to download or install the App Store", e)
                handler.post { onFailed(R.string.appstore_failed) }
            } finally {
                apk.delete()
            }
        }
    }

    @Throws(IOException::class)
    private fun download(destination: File) {
        val connection = URL(APPSTORE_APK_URL).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Unexpected HTTP response $responseCode")
            }
            val contentLength = connection.contentLengthLong
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var downloaded = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (Thread.currentThread().isInterrupted) {
                            throw IOException("Download interrupted")
                        }
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (contentLength > 0) {
                            val progress = (downloaded * 100 / contentLength).toInt()
                            handler.post { progressBar.progress = progress }
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    @Throws(IOException::class)
    private fun install(apk: File) {
        val installer = packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(APPSTORE_PACKAGE)
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            FileInputStream(apk).use { input ->
                session.openWrite(APK_FILE_NAME, 0, apk.length()).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    session.fsync(output)
                }
            }
            val intent = Intent(ACTION_INSTALL_RESULT).setPackage(packageName)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pendingIntent.intentSender)
        }
    }

    private fun handleInstallResult(intent: Intent) {
        val status = intent.getIntExtra(
            PackageInstaller.EXTRA_STATUS,
            PackageInstaller.STATUS_FAILURE,
        )
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmation = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                if (confirmation == null) {
                    onFailed(R.string.appstore_failed)
                    return
                }
                confirmation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(confirmation)
            }
            PackageInstaller.STATUS_SUCCESS -> onInstalled()
            else -> onFailed(R.string.appstore_failed)
        }
    }

    private fun onDownloadFinished() {
        progressBar.isIndeterminate = true
        statusView.setText(R.string.appstore_installing)
    }

    private fun onInstalled() {
        progressBar.isIndeterminate = false
        progressBar.visibility = View.GONE
        installButton.visibility = View.GONE
        statusView.setText(R.string.appstore_installed)
    }

    private fun onFailed(messageResId: Int) {
        progressBar.isIndeterminate = false
        progressBar.visibility = View.GONE
        installButton.isEnabled = true
        statusView.setText(messageResId)
    }

    override val layoutResId = R.layout.appstore_page

    override val titleResId = R.string.setup_appstore

    override val iconResId = R.drawable.ic_system_update

    companion object {
        private const val TAG = "AppStoreActivity"
        private const val APPSTORE_PACKAGE = "app.grapheneos.apps"
        private const val APPSTORE_APK_URL =
            "https://github.com/GrapheneOS/AppStore/releases/download/36/AppStore-36.apk"
        private const val APK_FILE_NAME = "AppStore.apk"
        private const val ACTION_INSTALL_RESULT =
            "org.lineageos.setupwizard.APPSTORE_INSTALL_RESULT"
        private const val BUFFER_SIZE = 8192
        private const val TIMEOUT_MS = 30000
    }
}
