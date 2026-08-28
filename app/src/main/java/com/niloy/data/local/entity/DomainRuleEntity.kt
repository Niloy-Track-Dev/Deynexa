package com.niloy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "domain_rules")
data class DomainRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domainPattern: String,
    val ruleType: String = "CLASSIFY",
    val category: String = "Unknown",
    val qualityRating: String = "UNRATED",
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
