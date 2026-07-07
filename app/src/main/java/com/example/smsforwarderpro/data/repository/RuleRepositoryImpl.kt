package com.example.smsforwarderpro.data.repository

import com.example.smsforwarderpro.data.local.db.ForwardingRule
import com.example.smsforwarderpro.data.local.db.ForwardingRuleDao
import com.example.smsforwarderpro.domain.model.Rule
import com.example.smsforwarderpro.domain.model.RuleCondition
import com.example.smsforwarderpro.domain.model.RuleLogicalOperator
import com.example.smsforwarderpro.domain.repository.RuleRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val ruleDao: ForwardingRuleDao
) : RuleRepository {

    private val gson = Gson()

    private data class ConditionsWrapper(
        val operator: RuleLogicalOperator,
        val conditions: List<RuleCondition>
    )

    override fun getAllRulesFlow(): Flow<List<Rule>> {
        return ruleDao.getAllRulesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getAllRules(): List<Rule> {
        return ruleDao.getAllRules().map { it.toDomain() }
    }

    override suspend fun saveRule(rule: Rule) {
        ruleDao.insertRule(rule.toEntity())
    }

    override suspend fun deleteRule(rule: Rule) {
        ruleDao.deleteRule(rule.toEntity())
    }

    override suspend fun updateRules(rules: List<Rule>) {
        ruleDao.insertRules(rules.map { it.toEntity() })
    }

    private fun ForwardingRule.toDomain(): Rule {
        val destinationsType = object : TypeToken<List<String>>() {}.type
        val destinationIds: List<String> = try {
            gson.fromJson(this.destinationIdsJson, destinationsType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        // Try parsing new conditions format (wrapper with operator and list)
        var conditionsList = emptyList<RuleCondition>()
        var logicalOp = RuleLogicalOperator.AND

        try {
            val wrapper = gson.fromJson(this.conditionsJson, ConditionsWrapper::class.java)
            if (wrapper != null) {
                conditionsList = wrapper.conditions
                logicalOp = wrapper.operator
            } else {
                // Fallback for legacy plain list format
                val legacyType = object : TypeToken<List<RuleCondition>>() {}.type
                val legacyList: List<RuleCondition>? = gson.fromJson(this.conditionsJson, legacyType)
                if (legacyList != null) {
                    conditionsList = legacyList
                }
            }
        } catch (e: Exception) {
            // Fallback for legacy format
            try {
                val legacyType = object : TypeToken<List<RuleCondition>>() {}.type
                val legacyList: List<RuleCondition>? = gson.fromJson(this.conditionsJson, legacyType)
                if (legacyList != null) {
                    conditionsList = legacyList
                }
            } catch (ex: Exception) {
                // Empty fallback
            }
        }

        return Rule(
            id = this.id,
            name = this.name,
            conditions = conditionsList,
            logicalOperator = logicalOp,
            destinationIds = destinationIds,
            priority = this.priority,
            isActive = this.isActive,
            breakOnMatch = this.breakOnMatch
        )
    }

    private fun Rule.toEntity(): ForwardingRule {
        val destinationsType = object : TypeToken<List<String>>() {}.type
        val destinationIdsJson = gson.toJson(this.destinationIds, destinationsType)

        val wrapper = ConditionsWrapper(this.logicalOperator, this.conditions)
        val conditionsJson = gson.toJson(wrapper)

        return ForwardingRule(
            id = this.id,
            name = this.name,
            conditionsJson = conditionsJson,
            destinationIdsJson = destinationIdsJson,
            priority = this.priority,
            isActive = this.isActive,
            breakOnMatch = this.breakOnMatch
        )
    }
}
