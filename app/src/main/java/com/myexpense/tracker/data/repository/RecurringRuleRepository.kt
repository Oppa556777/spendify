package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.RecurringRuleDao
import com.myexpense.tracker.data.database.entity.RecurringRuleEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRuleRepository @Inject constructor(
    private val dao: RecurringRuleDao,
) {

    suspend fun insert(rule: RecurringRuleEntity): Long = dao.insert(rule)

    suspend fun getById(id: Long) = dao.getById(id)

    suspend fun update(rule: RecurringRuleEntity) = dao.update(rule)

    suspend fun delete(id: Long) = dao.deleteById(id)
}
