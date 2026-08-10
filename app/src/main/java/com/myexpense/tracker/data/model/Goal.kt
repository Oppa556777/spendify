package com.myexpense.tracker.data.model

/** A savings goal the user is working towards. */
data class Goal(
    val id: Long = 0,
    val name: String,
    val targetAmount: Long,          // minor units
    val savedAmount: Long = 0,       // minor units
    val deadline: Long? = null,      // epoch millis
    val iconName: String = "star",
    val color: Long = 0xFF6C63FF,
    val accountId: Long? = null,
    val note: String? = null,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** Goal joined with its progress percentage. */
data class GoalWithProgress(
    val goal: Goal,
    val progress: Float,             // 0f..1f+
) {
    val percent: Int get() = (progress * 100).toInt().coerceIn(0, 999)
    val remaining: Long get() = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0)
}
