package com.example.smsforwarderpro.domain.usecase

import com.example.smsforwarderpro.domain.model.ConditionType
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.model.Rule
import com.example.smsforwarderpro.domain.model.RuleLogicalOperator
import javax.inject.Inject

class RuleEvaluator @Inject constructor() {

    fun evaluate(rule: Rule, message: Message): Boolean {
        if (!rule.isActive) return false
        if (rule.conditions.isEmpty()) return true

        return when (rule.logicalOperator) {
            RuleLogicalOperator.AND -> {
                rule.conditions.all { matchesCondition(it.type, it.value, message) }
            }
            RuleLogicalOperator.OR -> {
                rule.conditions.any { matchesCondition(it.type, it.value, message) }
            }
        }
    }

    private fun matchesCondition(type: ConditionType, value: String, message: Message): Boolean {
        return try {
            when (type) {
                ConditionType.SENDER_EXACT -> message.sender.trim() == value.trim()
                ConditionType.SENDER_PREFIX -> message.sender.trim().startsWith(value.trim())
                ConditionType.SENDER_REGEX -> {
                    val regex = Regex(value.trim(), RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(message.sender)
                }
                ConditionType.BODY_CONTAINS -> message.body.contains(value, ignoreCase = true)
                ConditionType.BODY_REGEX -> {
                    val regex = Regex(value, RegexOption.IGNORE_CASE)
                    regex.containsMatchIn(message.body)
                }
                ConditionType.TAG_MATCH -> message.tag.name.equals(value.trim(), ignoreCase = true)
                ConditionType.SIM_SLOT_MATCH -> message.simSlot.toString() == value.trim()
            }
        } catch (e: Exception) {
            false
        }
    }
}
