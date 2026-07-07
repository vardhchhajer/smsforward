package com.example.smsforwarderpro.ui.rules

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smsforwarderpro.domain.model.ConditionType
import com.example.smsforwarderpro.domain.model.Rule
import com.example.smsforwarderpro.domain.model.RuleCondition
import com.example.smsforwarderpro.domain.model.RuleLogicalOperator
import com.example.smsforwarderpro.theme.ColorDanger
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    viewModel: RulesViewModel,
    modifier: Modifier = Modifier
) {
    val rules by viewModel.rules.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Routing Rules",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (rules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No custom rules declared.",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Click the + button to create a route.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Rules are evaluated sequentially. Higher items take precedence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    itemsIndexed(rules) { index, rule ->
                        RuleCard(
                            rule = rule,
                            isFirst = index == 0,
                            isLast = index == rules.size - 1,
                            onToggleActive = { active -> viewModel.toggleRuleActive(rule, active) },
                            onDelete = { viewModel.deleteRule(rule) },
                            onMoveUp = {
                                val list = rules.toMutableList()
                                val temp = list[index]
                                list[index] = list[index - 1]
                                list[index - 1] = temp
                                viewModel.updateRulesPriority(list)
                            },
                            onMoveDown = {
                                val list = rules.toMutableList()
                                val temp = list[index]
                                list[index] = list[index + 1]
                                list[index + 1] = temp
                                viewModel.updateRulesPriority(list)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            if (showAddDialog) {
                AddRuleDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { newRule ->
                        viewModel.saveRule(newRule.copy(priority = rules.size))
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun RuleCard(
    rule: Rule,
    isFirst: Boolean,
    isLast: Boolean,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isActive) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.isActive) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Priority index: ${rule.priority}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = rule.isActive,
                        onCheckedChange = onToggleActive,
                        modifier = Modifier.scale(0.8f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ColorDanger
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Condition logic summary
            Text(
                text = "Conditions (${rule.logicalOperator}):",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            rule.conditions.forEach { cond ->
                Text(
                    text = "- ${cond.type.name}: \"${cond.value}\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destinations summary
            Text(
                text = "Target Channels:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            val destLabel = if (rule.destinationIds.isEmpty()) "DEFAULT (Matches OTP routing only)" 
                            else rule.destinationIds.joinToString(", ")
            Text(
                text = destLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            if (rule.breakOnMatch) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Breaks on match (stops further rules matching)",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Priority re-order actions footer
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up")
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down")
                }
            }
        }
    }
}

// Scale modifier helper for custom Switch size
fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
            placeable.placeRelative(0, 0)
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onSave: (Rule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf(RuleLogicalOperator.AND) }
    var breakOnMatch by remember { mutableStateOf(false) }

    // List of conditions to build
    val conditions = remember { mutableStateListOf<RuleCondition>() }
    var newCondType by remember { mutableStateOf(ConditionType.BODY_CONTAINS) }
    var newCondValue by remember { mutableStateOf("") }

    // Target destinations select
    val selectedDestinations = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Routing Rule", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Match logic:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = operator == RuleLogicalOperator.AND, onClick = { operator = RuleLogicalOperator.AND })
                        Text("AND")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = operator == RuleLogicalOperator.OR, onClick = { operator = RuleLogicalOperator.OR })
                        Text("OR")
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = breakOnMatch, onCheckedChange = { breakOnMatch = it })
                    Text("Break evaluation on match", style = MaterialTheme.typography.bodyMedium)
                }

                Divider()

                // Rule Conditions Section
                Text("Conditions:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                conditions.forEachIndexed { idx, cond ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${cond.type.name}: \"${cond.value}\"", style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = { conditions.removeAt(idx) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ColorDanger)
                        }
                    }
                }

                // Add condition fields
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Add condition field:", style = MaterialTheme.typography.labelSmall)
                        
                        // Condition selection row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf(ConditionType.SENDER_EXACT, ConditionType.BODY_CONTAINS, ConditionType.TAG_MATCH).forEach { type ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
                                    RadioButton(selected = newCondType == type, onClick = { newCondType = type })
                                    Text(type.name.take(12), fontSize = 9.sp)
                                }
                            }
                        }
                        
                        OutlinedTextField(
                            value = newCondValue,
                            onValueChange = { newCondValue = it },
                            label = { Text("Filter Value (Regex / Keyword)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Button(
                            onClick = {
                                if (newCondValue.isNotBlank()) {
                                    conditions.add(RuleCondition(newCondType, newCondValue))
                                    newCondValue = ""
                                }
                            },
                            modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                        ) {
                            Text("Add Field", fontSize = 11.sp)
                        }
                    }
                }

                Divider()

                // Target Destinations Section
                Text("Target Channels:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                listOf("TELEGRAM", "WHATSAPP", "WEBHOOK", "GMAIL", "SMS").forEach { dest ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = selectedDestinations.contains(dest),
                            onCheckedChange = { check ->
                                if (check) selectedDestinations.add(dest)
                                else selectedDestinations.remove(dest)
                            }
                        )
                        Text(dest, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (selectedDestinations.isEmpty()) {
                    Text(
                        text = "Select at least one target channel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && conditions.isNotEmpty() && selectedDestinations.isNotEmpty(),
                onClick = {
                    if (name.isNotBlank() && conditions.isNotEmpty() && selectedDestinations.isNotEmpty()) {
                        val rule = Rule(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            conditions = conditions.toList(),
                            logicalOperator = operator,
                            destinationIds = selectedDestinations.toList(),
                            priority = 0,
                            isActive = true,
                            breakOnMatch = breakOnMatch
                        )
                        onSave(rule)
                    }
                }
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
