package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.SubscriptionDao
import com.myexpense.tracker.data.database.entity.SubscriptionEntity
import com.myexpense.tracker.data.model.BillingCycle
import com.myexpense.tracker.data.model.Subscription
import com.myexpense.tracker.data.model.SubscriptionReminder
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toHexColor
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepository @Inject constructor(
    private val dao: SubscriptionDao,
    private val categoryRepository: CategoryRepository,
) {

    /** All subscriptions with their category names resolved. */
    fun observeAllWithCategories(): Flow<List<Subscription>> =
        combine(dao.observeAll(), categoryRepository.observeAll()) { subs, cats ->
            val catMap = cats.associateBy { it.id }
            subs.map { it.toModel(catMap[it.categoryId]?.name) }
        }

    /** Subscriptions due within [days] days from now (used for reminders). */
    fun observeDueWithin(days: Int): Flow<List<SubscriptionReminder>> {
        val now = System.currentTimeMillis()
        val windowEnd = now + days * 86_400_000L
        return dao.observeDueBetween(now, windowEnd).map { list ->
            list.map { it.toReminder(now) }
        }
    }

    suspend fun getById(id: Long): Subscription? =
        dao.getById(id)?.let { entity -> entity.toModel(null) }

    suspend fun save(subscription: Subscription): Long {
        val entity = subscription.toEntity()
        return if (subscription.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            subscription.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun toggleActive(id: Long, active: Boolean) {
        dao.getById(id)?.let { dao.update(it.copy(isActive = active)) }
    }

    /** Advances the next due date by one billing cycle ("Pay" = mark as noted). */
    suspend fun markNoted(id: Long) {
        val entity = dao.getById(id) ?: return
        dao.update(entity.copy(nextDueDate = advance(entity.nextDueDate, entity.billingCycle)))
    }

    suspend fun insertAll(subscriptions: List<Subscription>) =
        dao.insertAll(subscriptions.map { it.toEntity() })

    suspend fun deleteAll() = dao.deleteAll()

    private fun advance(date: Long, cycle: BillingCycle): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        when (cycle) {
            BillingCycle.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            BillingCycle.MONTHLY -> cal.add(Calendar.MONTH, 1)
            BillingCycle.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun SubscriptionEntity.toModel(categoryName: String?): Subscription = Subscription(
        id = id,
        name = name,
        amount = amount.toMinorUnits(),
        billingCycle = billingCycle,
        nextDueDate = nextDueDate,
        categoryId = categoryId,
        categoryName = categoryName,
        color = colorHex.toColorLong(),
        icon = iconName,
        reminderDays = reminderDays,
        isActive = isActive,
        note = note,
        createdAt = createdAt,
    )

    private fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
        id = id,
        name = name,
        amount = amount.toRupees(),
        billingCycle = billingCycle,
        nextDueDate = nextDueDate,
        categoryId = requireNotNull(categoryId) { "Subscription requires a category" },
        colorHex = color.toHexColor(),
        iconName = icon,
        reminderDays = reminderDays,
        isActive = isActive,
        note = note,
        createdAt = createdAt,
    )

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
