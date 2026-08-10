package com.myexpense.tracker.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myexpense.tracker.data.database.dao.AchievementDao
import com.myexpense.tracker.data.database.dao.AppSettingDao
import com.myexpense.tracker.data.database.entity.AchievementEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AchievementsUiState(
    val achievements: List<AchievementEntity> = emptyList(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val recentlyUnlockedIds: Set<Long> = emptySet(),
    val consistentDaysLeft: Int? = null,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    achievementDao: AchievementDao,
    appSettingDao: AppSettingDao,
) : ViewModel() {

    val uiState: StateFlow<AchievementsUiState> =
        achievementDao.observeAll().map { list ->
            val now = System.currentTimeMillis()
            val recentlyUnlocked = list.filter {
                it.isUnlocked && it.unlockedAt != null && now - it.unlockedAt!! < 60_000L
            }.map { it.id }.toSet()

            val firstOpen = runCatching { appSettingDao.getValue("first_open")?.toLongOrNull() }.getOrDefault(null)
            val consistentDaysLeft = if (firstOpen != null) {
                val elapsed = (now - firstOpen) / 86_400_000L
                (90L - elapsed).coerceAtLeast(0L).toInt()
            } else null

            AchievementsUiState(
                achievements = list,
                unlockedCount = list.count { it.isUnlocked },
                totalCount = list.size,
                recentlyUnlockedIds = recentlyUnlocked,
                consistentDaysLeft = consistentDaysLeft,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AchievementsUiState(),
        )
}
