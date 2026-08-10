package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.SubscriptionDao
import com.myexpense.tracker.data.database.entity.SubscriptionEntity
import com.myexpense.tracker.data.model.BillingCycle
import com.myexpense.tracker.data.model.SubscriptionReminder
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toMinorUnits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val dao: SubscriptionDao,
) {

    /** Subscriptions due within [days] days from now (used for reminders). */
    fun observeDueWithin(days: Int): Flow<List<SubscriptionReminder>> {
        val now = System.currentTimeMillis()
        val windowEnd = now + days * 86_400_000L
        return dao.observeDueBetween(now, windowEnd).map { list ->
            list.map { it.toReminder(now) }
        }
    }

    /** Advances the next due date by one billing cycle ("Pay" = mark as noted). */
    suspend fun markNoted(reminder: SubscriptionReminder) {
        val entity = dao.getById(reminder.id) ?: return
        dao.update(entity.copy(nextDueDate = advance(entity.nextDueDate, entity.billingCycle)))
    }

    private fun advance(date: Long, cycle: BillingCycle): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        when (cycle) {
            BillingCycle.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            BillingCycle.MONTHLY -> cal.add(Calendar.MONTH, 1)
            BillingCycle.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun SubscriptionEntity.toReminder(now: Long) = SubscriptionReminder(
        id = id,
        name = name,
        amount = amount.toMinorUnits(),
        nextDueDate = nextDueDate,
        daysLeft = (((nextDueDate - now) / 86_400_000L).toInt()).coerceAtLeast(0),
        color = colorHex.toColorLong(),
        icon = iconName,
        billingCycle = billingCycle,
    )
}
