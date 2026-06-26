package org.denistouch.youtubescreensaver

import android.os.Bundle
import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

/**
 * Экран настроек заставки: добавление видео по ссылке и выбор видео из списка.
 */
class ScreensaverSettingsActivity : AppCompatActivity() {

    private lateinit var store: VideoStore
    private lateinit var adapter: VideoAdapter
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private lateinit var urlInput: EditText
    private lateinit var emptyHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        store = VideoStore(this)
        urlInput = findViewById(R.id.urlInput)
        emptyHint = findViewById(R.id.emptyHint)

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
        findViewById<Button>(R.id.playButton).setOnClickListener { onOpenDreamSettings() }

        refreshList()
    }

    override fun onDestroy() {
        ioExecutor.shutdownNow()
        super.onDestroy()
    }

    private fun onAddClicked() {
        val input = urlInput.text.toString()
        val videoId = YoutubeUrlParser.extractVideoId(input)
        if (videoId == null) {
            Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_SHORT).show()
            return
        }

        // Сразу сохраняем с временным названием (id), название подтянем из сети в фоне.
        store.addVideo(Video(videoId, videoId))
        urlInput.text.clear()
        refreshList()

        ioExecutor.execute {
            val title = YoutubeTitleFetcher.fetchTitle(videoId)
            if (!title.isNullOrEmpty()) {
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    // Обновляем название, сохраняя текущий выбор.
                    val selected = store.getSelectedVideoId()
                    store.addVideo(Video(videoId, title))
                    store.setSelectedVideoId(selected)
                    refreshList()
                }
            }
        }
    }

    private fun onOpenDreamSettings() {
        // На Google TV / Android 12+ пункт выбора заставки убран из видимого меню,
        // но экран остаётся доступен по интенту. Пробуем несколько вариантов.
        val candidates = listOf(
            Intent("android.settings.DREAM_SETTINGS"),
            Intent(Settings.ACTION_DISPLAY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
        )
        for (intent in candidates) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // пробуем следующий
            }
        }
        Toast.makeText(this, R.string.dream_settings_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun refreshList() {
        val videos = store.getVideos()
        adapter.submit(videos, store.getSelectedVideoId())
        emptyHint.visibility = if (videos.isEmpty()) TextView.VISIBLE else TextView.GONE
    }
}
