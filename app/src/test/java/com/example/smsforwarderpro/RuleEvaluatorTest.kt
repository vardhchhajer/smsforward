package com.example.smsforwarderpro

import com.example.smsforwarderpro.domain.model.ConditionType
import com.example.smsforwarderpro.domain.model.Message
import com.example.smsforwarderpro.domain.model.MessageTag
import com.example.smsforwarderpro.domain.model.Rule
import com.example.smsforwarderpro.domain.model.RuleCondition
import com.example.smsforwarderpro.domain.model.RuleLogicalOperator
import com.example.smsforwarderpro.domain.usecase.RuleEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleEvaluatorTest {

    private val evaluator = RuleEvaluator()

    private fun createMessage(
        sender: String = "123456",
        body: String = "Test OTP message",
        tag: MessageTag = MessageTag.OTP,
        simSlot: Int = 0
    ) = Message(
        id = "msg1",
        sender = sender,
        body = body,
        tag = tag,
        timestamp = System.currentTimeMillis(),
        simSlot = simSlot
    )

    private fun createRule(
        conditions: List<RuleCondition>,
        logicalOperator: RuleLogicalOperator = RuleLogicalOperator.AND,
        isActive: Boolean = true
    ) = Rule(
        id = "rule1",
        name = "Test Rule",
        conditions = conditions,
        logicalOperator = logicalOperator,
        destinationIds = listOf("dest1"),
        priority = 1,
        isActive = isActive,
        breakOnMatch = false
    )

    @Test
    fun testInactiveRuleReturnsFalse() {
        val rule = createRule(
            conditions = listOf(RuleCondition(ConditionType.SENDER_EXACT, "123456")),
            isActive = false
        )
        val message = createMessage(sender = "123456")
        assertFalse(evaluator.evaluate(rule, message))
    }

    @Test
    fun testEmptyConditionsReturnsTrue() {
        val rule = createRule(conditions = emptyList())
        val message = createMessage()
        assertTrue(evaluator.evaluate(rule, message))
    }

    @Test
    fun testSenderExact() {
        val rule = createRule(listOf(RuleCondition(ConditionType.SENDER_EXACT, " 123456 ")))
        val matchingMessage = createMessage(sender = "123456")
        val nonMatchingMessage = createMessage(sender = "654321")

        assertTrue(evaluator.evaluate(rule, matchingMessage))
        assertFalse(evaluator.evaluate(rule, nonMatchingMessage))
    }

    @Test
    fun testSenderPrefix() {
        val rule = createRule(listOf(RuleCondition(ConditionType.SENDER_PREFIX, " +123 ")))
        val matchingMessage = createMessage(sender = "+1234567")
        val nonMatchingMessage = createMessage(sender = "123456")

        assertTrue(evaluator.evaluate(rule, matchingMessage))
        assertFalse(evaluator.evaluate(rule, nonMatchingMessage))
    }

    @Test
    fun testSenderRegex() {
        val rule = createRule(listOf(RuleCondition(ConditionType.SENDER_REGEX, "^(bank|otp)-.*$")))
        val matchingMessage = createMessage(sender = "BANK-SMS")
        val nonMatchingMessage = createMessage(sender = "MYBANK")

        assertTrue(evaluator.evaluate(rule, matchingMessage))
        assertFalse(evaluator.evaluate(rule, nonMatchingMessage))
    }

    @Test
    fun testBodyContains() {
        val rule = createRule(listOf(RuleCondition(ConditionType.BODY_CONTAINS, "verification")))
        val matchingMessage = createMessage(body = "Here is your VERIFICATION code: 9999")
        val nonMatchingMessage = createMessage(body = "Hello world")

        assertTrue(evaluator.evaluate(rule, matchingMessage))
        assertFalse(evaluator.evaluate(rule, nonMatchingMessage))
    }

    @Test
    fun testBodyRegex() {
        val rule = createRule(listOf(RuleCondition(ConditionType.BODY_REGEX, "code is [0-9]{4,6}")))
        val matchingMessage = createMessage(body = "Your authentication code is 12345")
        val nonMatchingMessage = createMessage(body = "Your authentication code is abcde")

        assertTrue(evaluator.evaluate(rule, matchingMessage))
        assertFalse(evaluator.evaluate(rule, nonMatchingMessage))
    }

    @Test
    fun testTagMatch() {
        val rule = createRule(listOf(RuleCondition(ConditionType.TAG_MATCH, "otp")))
        val matchingMessage = createMessage(tag = MessageTag.OTP)
        val nonMatchingMessage = createMessage(tag = MessageTag.SMS)

        assertTrue(evaluator.evaluate(rule, matchingMessage))
        assertFalse(evaluator.evaluate(rule, nonMatchingMessage))
    }

    @Test
    fun testSimSlotMatch() {
        val rule = createRule(listOf(RuleCondition(ConditionType.SIM_SLOT_MATCH, "1")))
        val matchingMessage = createMessage(simSlot = 1)
        val nonMatchingMessage = createMessage(simSlot = 0)

        assertTrue(evaluator.evaluate(rule, matchingMessage))
        assertFalse(evaluator.evaluate(rule, nonMatchingMessage))
    }

    @Test
    fun testAndLogicalOperator() {
        val rule = createRule(
            conditions = listOf(
                RuleCondition(ConditionType.SENDER_EXACT, "123456"),
                RuleCondition(ConditionType.BODY_CONTAINS, "verification")
            ),
            logicalOperator = RuleLogicalOperator.AND
        )

        val bothMatch = createMessage(sender = "123456", body = "your verification code is 123")
        val oneMatches = createMessage(sender = "123456", body = "hello world")
        val noneMatches = createMessage(sender = "999", body = "hello world")

        assertTrue(evaluator.evaluate(rule, bothMatch))
        assertFalse(evaluator.evaluate(rule, oneMatches))
        assertFalse(evaluator.evaluate(rule, noneMatches))
    }

    @Test
    fun testOrLogicalOperator() {
        val rule = createRule(
            conditions = listOf(
                RuleCondition(ConditionType.SENDER_EXACT, "123456"),
                RuleCondition(ConditionType.BODY_CONTAINS, "verification")
            ),
            logicalOperator = RuleLogicalOperator.OR
        )

        val bothMatch = createMessage(sender = "123456", body = "your verification code is 123")
        val oneMatches = createMessage(sender = "123456", body = "hello world")
        val noneMatches = createMessage(sender = "999", body = "hello world")

        assertTrue(evaluator.evaluate(rule, bothMatch))
        assertTrue(evaluator.evaluate(rule, oneMatches))
        assertFalse(evaluator.evaluate(rule, noneMatches))
    }

    @Test
    fun testInvalidRegexDoesNotCrash() {
        val rule = createRule(listOf(RuleCondition(ConditionType.BODY_REGEX, "[a-z")))
        val message = createMessage(body = "test")
        // Should not crash, just return false
        assertFalse(evaluator.evaluate(rule, message))
    }
}
