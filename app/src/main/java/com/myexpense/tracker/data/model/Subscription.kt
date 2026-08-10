package com.myexpense.tracker.data.model

/** A recurring subscription the user pays for. */
data class Subscription(
    val id: Long = 0,
    val name: String,
    val amount: Long = 0,           // minor units
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val nextDueDate: Long = 0,      // epoch millis
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val color: Long = 0xFF6C63FF,
    val icon: String = "subscriptions",
    val reminderDays: Int = 3,
    val isActive: Boolean = true,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

val Subscription.daysLeft: Int
    get() = (((nextDueDate - System.currentTimeMillis()) / 86_400_000L).toInt()).coerceAtLeast(0)

/** Urgency tier used to color the due chip. */
enum class DueUrgency { FAR, SOON, URGENT }

val Subscription.urgency: DueUrgency
    get() = when {
        daysLeft < 3 -> DueUrgency.URGENT
        daysLeft <= 7 -> DueUrgency.SOON
        else -> DueUrgency.FAR
    }

/** Converts one billing cycle to its monthly equivalent factor. */
val BillingCycle.monthlyFactor: Double
    get() = when (this) {
        BillingCycle.WEEKLY -> 4.33
        BillingCycle.MONTHLY -> 1.0
        BillingCycle.YEARLY -> 1.0 / 12.0
    }
