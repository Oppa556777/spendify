package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.AccountDao
import com.myexpense.tracker.data.database.dao.RecurringRuleDao
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.AccountEntity
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountWithBalance
import com.myexpense.tracker.utils.toColorLong
import com.myexpense.tracker.utils.toHexColor
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val dao: AccountDao,
    private val transactionDao: TransactionDao,
    private val recurringRuleDao: RecurringRuleDao,
) {

    fun observeAll(): Flow<List<Account>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    /** Every account is active in this schema (no archived concept). */
    fun observeActive(): Flow<List<Account>> = observeAll()

    /** Accounts joined with their stored balance. */
    fun observeActiveWithBalance(): Flow<List<AccountWithBalance>> =
        dao.observeAll().map { list ->
            list.map { entity -> AccountWithBalance(account = entity.toModel(), balance = entity.balance.toMinorUnits()) }
        }

    fun observeTotalBalance(): Flow<Long> =
        observeActiveWithBalance().map { list -> list.sumOf { it.balance } }

    suspend fun getActive(): List<Account> = dao.getAll().map { it.toModel() }

    suspend fun getAll(): List<Account> = dao.getAll().map { it.toModel() }

    suspend fun getById(id: Long): Account? = dao.getById(id)?.toModel()

    suspend fun save(account: Account): Long {
        val entity = account.toEntity()
        return if (account.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            account.id
        }
    }

    /**
     * Safe delete: transactions and recurring rules are reassigned to another
     * account. Returns false when no other account exists (nothing deleted).
     */
    suspend fun delete(id: Long): Boolean {
        val fallback = dao.getAll().firstOrNull { it.id != id } ?: return false
        transactionDao.reassignAccount(id, fallback.id)
        recurringRuleDao.reassignAccount(id, fallback.id)
        dao.deleteById(id)
        return true
    }

    /** Applies a delta (minor units) to an account's stored balance. */
    suspend fun adjustBalance(accountId: Long, deltaMinor: Long) {
        val account = dao.getById(accountId) ?: return
        dao.update(account.copy(balance = account.balance + deltaMinor.toRupees()))
    }

    suspend fun insertAll(accounts: List<Account>) =
        dao.insertAll(accounts.map { it.toEntity() })

    suspend fun deleteAll() = dao.deleteAll()

    private fun AccountEntity.toModel() = Account(
        id = id,
        name = name,
        type = accountType,
        balance = balance.toMinorUnits(),
        currency = currency,
        color = colorHex.toColorLong(),
        icon = iconName,
        isDefault = isDefault,
        createdAt = createdAt,
    )

    private fun Account.toEntity() = AccountEntity(
        id = id,
        name = name,
        accountType = type,
        balance = balance.toRupees(),
        currency = currency,
        colorHex = color.toHexColor(),
        iconName = icon,
        isDefault = isDefault,
        createdAt = createdAt,
    )
}
