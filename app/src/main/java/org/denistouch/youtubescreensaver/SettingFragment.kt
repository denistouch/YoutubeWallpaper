package org.denistouch.youtubescreensaver

import android.os.Bundle
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // Опционально: можно добавить обработчик изменения
        findPreference<EditTextPreference>("youtube.id")?.setOnPreferenceChangeListener { _, newValue ->
            Toast.makeText(requireContext(), "Идентификатор видео: $newValue", Toast.LENGTH_SHORT).show()
            true
        }
    }
}