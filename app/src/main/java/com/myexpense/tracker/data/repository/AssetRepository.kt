package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.AssetDao
import com.myexpense.tracker.data.database.entity.AssetEntity
import com.myexpense.tracker.data.model.AssetType
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Asset joined with computed invested / value / gain. */
data class AssetView(
    val id: Long,
    val name: String,
    val type: AssetType,
    val quantity: Double,
    val buyPrice: Long,       // minor units per unit
    val currentPrice: Long,   // minor units per unit
    val note: String?,
    val invested: Long,       // quantity × buyPrice
    val value: Long,          // quantity × currentPrice
) {
    val gain: Long get() = value - invested
    val gainPercent: Double
        get() = if (invested > 0) (value - invested).toDouble() / invested * 100.0 else 0.0
}

data class PortfolioSummary(
    val invested: Long = 0,
    val value: Long = 0,
    val gain: Long = 0,
    val gainPercent: Double = 0.0,
)

@Singleton
class AssetRepository @Inject constructor(
    private val dao: AssetDao,
) {

    fun observeAll(): Flow<List<AssetView>> =
        dao.observeAll().map { list -> list.map { it.toView() } }

    fun observeSummary(): Flow<PortfolioSummary> =
        observeAll().map { assets ->
            val invested = assets.sumOf { it.invested }
            val value = assets.sumOf { it.value }
            PortfolioSummary(
                invested = invested,
                value = value,
                gain = value - invested,
                gainPercent = if (invested > 0) (value - invested).toDouble() / invested * 100.0 else 0.0,
            )
        }

    /** Portfolio summary + assets in one combine. */
    fun observePortfolio(): Flow<Pair<List<AssetView>, PortfolioSummary>> =
        combine(observeAll(), observeSummary()) { assets, summary -> assets to summary }

    suspend fun getById(id: Long): AssetView? = dao.getById(id)?.toView()

    suspend fun save(
        id: Long,
        name: String,
        type: AssetType,
        quantity: Double,
        buyPriceMinor: Long,
        currentPriceMinor: Long,
        note: String?,
    ): Long {
        val entity = AssetEntity(
            id = id,
            name = name,
            type = type,
            quantity = quantity,
            buyPrice = buyPriceMinor.toRupees(),
            currentPrice = currentPriceMinor.toRupees(),
            note = note,
        )
        return if (id == 0L) dao.insert(entity) else {
            dao.update(entity)
            id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    private fun AssetEntity.toView() = AssetView(
        id = id,
        name = name,
        type = type,
        quantity = quantity,
        buyPrice = buyPrice.toMinorUnits(),
        currentPrice = currentPrice.toMinorUnits(),
        note = note,
        invested = (quantity * buyPrice).toLong(),
        value = (quantity * currentPrice).toLong(),
    )
}
