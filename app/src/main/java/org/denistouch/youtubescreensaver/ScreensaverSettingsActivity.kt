package org.denistouch.youtubescreensaver

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.IOException
import java.util.concurrent.Executors

class ScreensaverSettingsActivity : AppCompatActivity() {

    private lateinit var store: VideoStore
    private lateinit var adapter: VideoAdapter
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private lateinit var urlInput: EditText
    private lateinit var manualAddBlock: View
    private lateinit var emptyHint: TextView
    private lateinit var statusText: TextView
    private lateinit var permissionHint: TextView
    private lateinit var timeoutButton: Button

    private var phoneAddServer: PhoneAddServer? = null
    private var phoneAddDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        store = VideoStore(this)
        urlInput = findViewById(R.id.urlInput)
        manualAddBlock = findViewById(R.id.manualAddBlock)
        emptyHint = findViewById(R.id.emptyHint)
        statusText = findViewById(R.id.screensaverStatus)
        permissionHint = findViewById(R.id.permissionHint)
        timeoutButton = findViewById(R.id.timeoutButton)

        adapter = VideoAdapter(
            videos = store.getVideos(),
            selectedId = store.getSelectedVideoId(),
            onSelect = { video ->
                store.setSelectedVideoId(video.id)
                refreshList()
                Toast.makeText(this, getString(R.string.selected_toast, video.title), Toast.LENGTH_SHORT).show()
            },
            onDelete = { video ->
                store.removeVideo(video.id)
                refreshList()
            },
        )

        findViewById<RecyclerView>(R.id.videoList).apply {
            layoutManager = LinearLayoutManager(this@ScreensaverSettingsActivity)
            adapter = this@ScreensaverSettingsActivity.adapter
        }

        findViewById<Button>(R.id.addButton).setOnClickListener { onAddClicked() }
        findViewById<Button>(R.id.showManualInputButton).setOnClickListener { toggleManualInput() }
        findViewById<Button>(R.id.addFromPhoneButton).setOnClickListener { onAddFromPhone() }
        findViewById<Button>(R.id.playButton).setOnClickListener { onOpenDreamSettings() }
        findViewById<Button>(R.id.makeActiveButton).setOnClickListener { onMakeActive() }
        findViewById<Button>(R.id.timeoutButton).setOnClickListener { showTimeoutDialog() }

        refreshList()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateTimeoutButton()
    }

    override fun onPause() {
        super.onPause()
        phoneAddDialog?.dismiss()
    }

    override fun onDestroy() {
        stopPhoneServer()
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun toggleManualInput() {
        manualAddBlock.visibility =
            if (manualAddBlock.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun onAddClicked() {
        val input = urlInput.text.toString()
        val videoId = YoutubeUrlParser.extractVideoId(input)
        if (videoId == null) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            return
        }

        store.addVideo(Video(videoId, videoId))
        urlInput.text.clear()
        manualAddBlock.visibility = View.GONE
        refreshList()

        ioExecutor.execute {
            val title = YoutubeTitleFetcher.fetchTitle(videoId)
            if (!title.isNullOrEmpty()) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    val selected = store.getSelectedVideoId()
                    store.addVideo(Video(videoId, title))
                    store.setSelectedVideoId(selected)
                    refreshList()
                }
            }
        }
    }

    private fun onAddFromPhone() {
        val ip = PhoneAddServer.localIp()
        if (ip == null) {
            Toast.makeText(this, R.string.phone_add_no_network, Toast.LENGTH_LONG).show()
            return
        }

        val server = PhoneAddServer { video ->
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                store.addVideo(video)
                refreshList()
                Toast.makeText(this, getString(R.string.phone_add_added, video.title), Toast.LENGTH_SHORT).show()
            }
        }

        try {
            server.start()
        } catch (e: IOException) {
            Toast.makeText(
                this,
                getString(R.string.phone_add_server_error, PhoneAddServer.PORT),
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        phoneAddServer = server

        val url = "http://$ip:${PhoneAddServer.PORT}/"

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_phone, null)
        val qrImage = dialogView.findViewById<ImageView>(R.id.qrImage)
        dialogView.findViewById<TextView>(R.id.serverUrl).text = url

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.phone_add_dialog_title)
            .setView(dialogView)
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { stopPhoneServer() }
            .show()
        phoneAddDialog = dialog

        ioExecutor.execute {
            val bmp = PhoneAddServer.qrBitmap(url)
            runOnUiThread {
                if (!dialog.isShowing) return@runOnUiThread
                qrImage.setImageBitmap(bmp)
            }
        }
    }

    private fun stopPhoneServer() {
        phoneAddServer?.stop()
        phoneAddServer = null
        phoneAddDialog = null
    }

    private fun onOpenDreamSettings() {
        val candidates = listOf(
            Intent().setClassName(
                "com.android.tv.settings",
                "com.android.tv.settings.device.display.daydream.DaydreamActivity",
            ),
            Intent("android.settings.DREAM_SETTINGS"),
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
        Toast.makeText(this, R.string.dream_settings_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun refreshList() {
        val videos = store.getVideos()
        adapter.submit(videos, store.getSelectedVideoId())
        emptyHint.visibility = if (videos.isEmpty()) TextView.VISIBLE else TextView.GONE
    }

    private fun screensaverComponent(): String =
        ComponentName(this, VideoScreensaverService::class.java).flattenToString()

    private fun hasSecurePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    private fun adbGrantCommand(): String =
        "adb shell pm grant $packageName ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    private fun onMakeActive() {
        if (!hasSecurePermission()) {
            showPermissionHint()
            Toast.makeText(this, R.string.make_active_no_perm, Toast.LENGTH_LONG).show()
            return
        }
        try {
            val cr = contentResolver
            Settings.Secure.putString(cr, "screensaver_components", screensaverComponent())
            Settings.Secure.putInt(cr, "screensaver_enabled", 1)
            Settings.Secure.putInt(cr, "screensaver_activate_on_sleep", 1)
            Toast.makeText(this, R.string.make_active_ok, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, e.message ?: "Ошибка", Toast.LENGTH_LONG).show()
        }
        updateStatus()
    }

    private fun showPermissionHint() {
        permissionHint.text = getString(R.string.status_perm_missing, adbGrantCommand())
        permissionHint.visibility = View.VISIBLE
    }

    private fun updateStatus() {
        val current = try {
            Settings.Secure.getString(contentResolver, "screensaver_components")
        } catch (_: Exception) {
            null
        }
        val mineCn = ComponentName(this, VideoScreensaverService::class.java)
        val isMine = current?.let { ComponentName.unflattenFromString(it) } == mineCn ||
            current == mineCn.flattenToShortString()
        statusText.text = when {
            current.isNullOrEmpty() -> getString(R.string.status_none)
            isMine -> getString(R.string.status_active)
            else -> getString(R.string.status_other)
        }
        if (hasSecurePermission()) {
            permissionHint.visibility = View.GONE
        }
    }

    private fun updateTimeoutButton() {
        timeoutButton.text = getString(R.string.timeout_value, currentTimeoutMinutes())
    }

    private fun showTimeoutDialog() {
        val labels = TIMEOUT_PRESETS.map { getString(R.string.timeout_value, it) }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.timeout_label)
            .setSingleChoiceItems(labels, closestPresetIndex(currentTimeoutMinutes())) { dialog, which ->
                val m = TIMEOUT_PRESETS[which]
                try {
                    Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, m * 60_000)
                    Toast.makeText(this, getString(R.string.timeout_ok, m), Toast.LENGTH_SHORT).show()
                    updateTimeoutButton()
                } catch (_: SecurityException) {
                    showPermissionHint()
                    Toast.makeText(this, R.string.make_active_no_perm, Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            }
            .show()
    }

    private fun currentTimeoutMinutes(): Int {
        val ms = try {
            Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, DEFAULT_TIMEOUT_MS)
        } catch (_: Exception) {
            DEFAULT_TIMEOUT_MS
        }
        return (ms / 60_000).coerceAtLeast(1)
    }

    private fun closestPresetIndex(minutes: Int): Int =
        TIMEOUT_PRESETS.indices.minByOrNull { kotlin.math.abs(TIMEOUT_PRESETS[it] - minutes) } ?: 0

    private companion object {
        val TIMEOUT_PRESETS = listOf(6, 10, 15, 20, 30, 45, 60, 90, 120)
        const val DEFAULT_TIMEOUT_MS = 360_000
    }
}
