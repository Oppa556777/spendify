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

    /** Rules that are due to fire as of [now] (active, within dates, interval elapsed). */
    suspend fun getDueRules(now: Long): List<RecurringRuleEntity> =
        dao.getActive().filter { rule ->
            rule.startDate <= now &&
                (rule.endDate == null || rule.endDate >= now) &&
                (rule.lastExecuted == null || rule.lastExecuted + intervalMillis(rule) <= now)
        }

    suspend fun markExecuted(id: Long, now: Long) {
        dao.getById(id)?.let { dao.update(it.copy(lastExecuted = now)) }
    }

    companion object {
        fun intervalMillis(rule: RecurringRuleEntity): Long =
            when (rule.frequency) {
                com.myexpense.tracker.data.model.RecurringFrequency.DAILY -> 86_400_000L
                com.myexpense.tracker.data.model.RecurringFrequency.WEEKLY -> 7L * 86_400_000L
                com.myexpense.tracker.data.model.RecurringFrequency.MONTHLY -> 30L * 86_400_000L
                com.myexpense.tracker.data.model.RecurringFrequency.YEARLY -> 365L * 86_400_000L
            } * rule.interval.coerceAtLeast(1)
    }
}
