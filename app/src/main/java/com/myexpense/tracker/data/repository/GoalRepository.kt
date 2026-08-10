package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.GoalDao
import com.myexpense.tracker.data.database.entity.GoalEntity
import com.myexpense.tracker.data.model.Goal
import com.myexpense.tracker.data.model.GoalWithProgress
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toHexColor
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val dao: GoalDao,
) {

    /** Goals with their progress percentage (computed in the mapping). */
    fun observeAll(): Flow<List<GoalWithProgress>> =
        dao.observeAll().map { list -> list.map { it.toModelWithProgress() } }

    suspend fun getById(id: Long): Goal? = dao.getById(id)?.toModel()

    suspend fun save(goal: Goal): Long {
        val entity = goal.toEntity()
        return if (goal.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            goal.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun insertAll(goals: List<Goal>) = dao.insertAll(goals.map { it.toEntity() })

    suspend fun deleteAll() = dao.deleteAll()

    private fun GoalEntity.toModel() = Goal(
        id = id,
        name = name,
        targetAmount = targetAmount.toMinorUnits(),
        savedAmount = savedAmount.toMinorUnits(),
        deadline = deadline,
        iconName = iconName,
        color = colorHex.toColorLong(),
        accountId = accountId,
        note = note,
        isCompleted = isCompleted,
        createdAt = createdAt,
    )

    private fun GoalEntity.toModelWithProgress() = GoalWithProgress(
        goal = toModel(),
        progress = if (targetAmount > 0) (savedAmount / targetAmount).toFloat() else 0f,
    )

    private fun Goal.toEntity() = GoalEntity(
        id = id,
        name = name,
        targetAmount = targetAmount.toRupees(),
        savedAmount = savedAmount.toRupees(),
        deadline = deadline,
        iconName = iconName,
        colorHex = color.toHexColor(),
        accountId = accountId,
        note = note,
        isCompleted = isCompleted,
        createdAt = createdAt,
    )
}
