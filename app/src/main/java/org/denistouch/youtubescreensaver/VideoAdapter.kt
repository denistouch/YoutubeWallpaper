package org.denistouch.youtubescreensaver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load

/**
 * Список сохранённых видео. Нажатие на строку делает видео выбранным,
 * кнопка удаления — убирает его из списка.
 */
class VideoAdapter(
    private var videos: List<Video>,
    private var selectedId: String?,
    private val onSelect: (Video) -> Unit,
    private val onDelete: (Video) -> Unit,
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val title: TextView = view.findViewById(R.id.videoTitle)
        val selectedMark: ImageView = view.findViewById(R.id.selectedMark)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.video_item, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.title.text = video.title
        holder.thumbnail.load("https://i.ytimg.com/vi/${video.id}/mqdefault.jpg")
        val isSelected = video.id == selectedId
        holder.selectedMark.visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
        holder.itemView.isSelected = isSelected
        holder.itemView.setOnClickListener { onSelect(video) }
        holder.deleteButton.setOnClickListener { onDelete(video) }
    }

    override fun getItemCount(): Int = videos.size

    fun submit(videos: List<Video>, selectedId: String?) {
        this.videos = videos
        this.selectedId = selectedId
        notifyDataSetChanged()
    }
}
