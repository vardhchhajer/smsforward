package com.example.smsforwarderpro.domain.model

enum class ConditionType {
    SENDER_EXACT, SENDER_PREFIX, SENDER_REGEX,
    BODY_CONTAINS, BODY_REGEX,
    TAG_MATCH, SIM_SLOT_MATCH
}

data class RuleCondition(
    val type: ConditionType,
    val value: String
)

enum class RuleLogicalOperator {
    AND, OR
}

data class Rule(
    val id: String,
    val name: String,
    val conditions: List<RuleCondition>,
    val logicalOperator: RuleLogicalOperator,
    val destinationIds: List<String>,
    val priority: Int,
    val isActive: Boolean,
    val breakOnMatch: Boolean
)
