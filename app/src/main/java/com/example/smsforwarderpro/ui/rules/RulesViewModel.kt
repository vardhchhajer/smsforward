package com.example.smsforwarderpro.ui.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsforwarderpro.domain.model.Rule
import com.example.smsforwarderpro.domain.repository.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository
) : ViewModel() {

    val rules: StateFlow<List<Rule>> = ruleRepository.getAllRulesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRule(rule: Rule) {
        viewModelScope.launch {
            ruleRepository.saveRule(rule)
        }
    }

    fun deleteRule(rule: Rule) {
        viewModelScope.launch {
            ruleRepository.deleteRule(rule)
        }
    }

    fun toggleRuleActive(rule: Rule, isActive: Boolean) {
        viewModelScope.launch {
            ruleRepository.saveRule(rule.copy(isActive = isActive))
        }
    }

    fun updateRulesPriority(reorderedList: List<Rule>) {
        viewModelScope.launch {
            // Assign priorities based on index position
            val updated = reorderedList.mapIndexed { index, rule ->
                rule.copy(priority = index)
            }
            ruleRepository.updateRules(updated)
        }
    }
}
