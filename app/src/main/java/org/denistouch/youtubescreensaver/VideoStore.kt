package org.denistouch.youtubescreensaver

import android.content.Context
import androidx.preference.PreferenceManager
import org.json.JSONArray
import org.json.JSONObject

/**
 * Хранилище списка видео и текущего выбранного видео в SharedPreferences.
 *
 * Список сериализуется в JSON-массив объектов {id, title}. Выбранное видео хранится
 * отдельным ключом, чтобы [VideoScreensaverService] мог быстро прочитать его без разбора списка.
 */
class VideoStore(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    /** Возвращает сохранённый список видео (может быть пустым). */
    fun getVideos(): List<Video> {
        val raw = prefs.getString(KEY_VIDEOS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Video(obj.getString("id"), obj.getString("title"))
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Добавляет видео в список. Если видео с таким id уже есть — обновляет название
     * и поднимает его наверх. Делает добавленное видео выбранным.
     */
    fun addVideo(video: Video) {
        val current = getVideos().filterNot { it.id == video.id }
        val updated = listOf(video) + current
        saveVideos(updated)
        setSelectedVideoId(video.id)
    }

    /** Удаляет видео из списка. Если оно было выбранным — выбирает первое из оставшихся. */
    fun removeVideo(id: String) {
        val updated = getVideos().filterNot { it.id == id }
        saveVideos(updated)
        if (getSelectedVideoId() == id) {
            setSelectedVideoId(updated.firstOrNull()?.id)
        }
    }

    fun getSelectedVideoId(): String? = prefs.getString(KEY_SELECTED, null)

    fun setSelectedVideoId(id: String?) {
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    private fun saveVideos(videos: List<Video>) {
        val array = JSONArray()
        videos.forEach { video ->
            array.put(
                JSONObject()
                    .put("id", video.id)
                    .put("title", video.title)
            )
        }
        prefs.edit().putString(KEY_VIDEOS, array.toString()).apply()
    }

    companion object {
        private const val KEY_VIDEOS = "videos.list"
        private const val KEY_SELECTED = "videos.selected"
    }
}
