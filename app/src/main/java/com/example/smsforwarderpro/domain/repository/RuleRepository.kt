package com.example.smsforwarderpro.domain.repository

import com.example.smsforwarderpro.domain.model.Rule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun getAllRulesFlow(): Flow<List<Rule>>
    suspend fun getAllRules(): List<Rule>
    suspend fun saveRule(rule: Rule)
    suspend fun deleteRule(rule: Rule)
    suspend fun updateRules(rules: List<Rule>)
}
