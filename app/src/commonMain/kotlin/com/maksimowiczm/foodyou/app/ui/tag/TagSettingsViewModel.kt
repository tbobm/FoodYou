package com.maksimowiczm.foodyou.app.ui.tag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maksimowiczm.foodyou.common.domain.tag.Tag
import com.maksimowiczm.foodyou.common.domain.tag.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** PRD 3.5 (categorisation). Backs the tag management settings screen: create/rename/delete. */
internal class TagSettingsViewModel(private val tagRepository: TagRepository) : ViewModel() {

    val tags: StateFlow<List<Tag>?> =
        tagRepository
            .observeTags()
            .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(2_000), initialValue = null)

    fun createTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return
        }

        viewModelScope.launch { tagRepository.createTag(trimmed) }
    }

    fun renameTag(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return
        }

        viewModelScope.launch { tagRepository.renameTag(id, trimmed) }
    }

    fun deleteTag(id: Long) {
        viewModelScope.launch { tagRepository.deleteTag(id) }
    }
}
