package org.denistouch.youtubescreensaver

/**
 * Сохранённое видео из списка пользователя.
 *
 * @param id    11-символьный идентификатор видео YouTube.
 * @param title человекочитаемое название (из oEmbed) либо сам id, если получить не удалось.
 */
data class Video(
    val id: String,
    val title: String,
)
