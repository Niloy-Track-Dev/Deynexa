package com.niloy.data.local.dao

import androidx.room.*
import com.niloy.data.local.entity.DomainRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainRuleDao {

    @Query("SELECT * FROM domain_rules ORDER BY createdAt DESC")
    fun getAllRules(): Flow<List<DomainRuleEntity>>

    @Query("SELECT * FROM domain_rules WHERE isEnabled = 1")
    suspend fun getActiveRules(): List<DomainRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: DomainRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<DomainRuleEntity>)

    @Update
    suspend fun updateRule(rule: DomainRuleEntity)

    @Delete
    suspend fun deleteRule(rule: DomainRuleEntity)

    @Query("DELETE FROM domain_rules WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM domain_rules")
    suspend fun deleteAllRules()
}
