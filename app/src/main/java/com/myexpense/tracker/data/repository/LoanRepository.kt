package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.LoanDao
import com.myexpense.tracker.data.database.entity.LoanEntity
import com.myexpense.tracker.data.model.Loan
import com.myexpense.tracker.data.model.LoanType
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val dao: LoanDao,
) {

    fun observeAll(): Flow<List<Loan>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    /** Total original lent and borrowed (all-time). */
    fun observeTotals(): Flow<Pair<Long, Long>> =
        combine(dao.observeTotalLent(), dao.observeTotalBorrowed()) { lent, borrowed ->
            lent.toMinorUnits() to borrowed.toMinorUnits()
        }

    suspend fun getById(id: Long): Loan? = dao.getById(id)?.toModel()

    suspend fun save(loan: Loan): Long {
        val entity = loan.toEntity()
        return if (loan.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            loan.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun markSettled(id: Long) = dao.markSettled(id)

    /** Adds a partial payment (or repayment for borrowed loans). */
    suspend fun addPayment(id: Long, amountMinor: Long) {
        val loan = dao.getById(id) ?: return
        val newPaid = (loan.paidAmount.toMinorUnits() + amountMinor).coerceAtMost(loan.amount.toMinorUnits())
        dao.updatePaidAmount(id, newPaid.toRupees())
        if (newPaid >= loan.amount.toMinorUnits()) dao.markSettled(id)
    }

    suspend fun insertAll(loans: List<Loan>) = dao.insertAll(loans.map { it.toEntity() })

    suspend fun deleteAll() = dao.deleteAll()

    private fun LoanEntity.toModel() = Loan(
        id = id,
        type = type,
        personName = personName,
        amount = amount.toMinorUnits(),
        paidAmount = paidAmount.toMinorUnits(),
        date = date.takeIf { it > 0 } ?: createdAt,
        dueDate = dueDate,
        note = note,
        isSettled = isSettled,
        createdAt = createdAt,
    )

    private fun Loan.toEntity() = LoanEntity(
        id = id,
        type = type,
        personName = personName,
        amount = amount.toRupees(),
        paidAmount = paidAmount.toRupees(),
        date = date,
        dueDate = dueDate,
        note = note,
        isSettled = isSettled,
        createdAt = createdAt,
    )
}
