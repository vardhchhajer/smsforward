package com.example.smsforwarderpro.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.model.MessageTag
import com.example.smsforwarderpro.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val allLogs = messageRepository.getAllMessageLogsFlow()

    val searchQuery = MutableStateFlow("")
    val selectedTagFilter = MutableStateFlow<MessageTag?>(null)

    val filteredLogs: StateFlow<List<Message>> = combine(
        allLogs,
        searchQuery,
        selectedTagFilter
    ) { logs, query, tag ->
        logs.filter { log ->
            val matchesQuery = query.isBlank() || 
                log.sender.contains(query, ignoreCase = true) || 
                log.body.contains(query, ignoreCase = true)
            
            val matchesTag = tag == null || log.tag == tag

            matchesQuery && matchesTag
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearAllLogs() {
        viewModelScope.launch {
            messageRepository.clearAllLogs()
        }
    }
}
