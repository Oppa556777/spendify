package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.AccountDao
import com.myexpense.tracker.data.database.dao.TransactionDao
import com.myexpense.tracker.data.database.entity.AccountEntity
import com.myexpense.tracker.data.model.Account
import com.myexpense.tracker.data.model.AccountWithBalance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val dao: AccountDao,
    private val transactionDao: TransactionDao,
) {

    fun observeActive(): Flow<List<Account>> =
        dao.observeActive().map { list -> list.map { it.toModel() } }

    fun observeAll(): Flow<List<Account>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    /** Accounts joined with their live balance (initial balance + all transactions). */
    fun observeActiveWithBalance(): Flow<List<AccountWithBalance>> =
        combine(dao.observeActive(), transactionDao.observeBalanceByAccount()) { accounts, deltas ->
            val deltaMap = deltas.associate { it.accountId to it.total }
            accounts.map { account ->
                AccountWithBalance(
                    account = account.toModel(),
                    balance = account.initialBalance + (deltaMap[account.id] ?: 0L),
                )
            }
        }

    fun observeTotalBalance(): Flow<Long> =
        observeActiveWithBalance().map { list -> list.sumOf { it.balance } }

    suspend fun getActive(): List<Account> = dao.getActive().map { it.toModel() }

    suspend fun getAll(): List<Account> = dao.observeAll().first().map { it.toModel() }

    suspend fun getById(id: Long): Account? = dao.getById(id)?.toModel()

    suspend fun save(account: Account): Long {
        val entity = account.toEntity()
        return if (account.id == 0L) dao.insert(entity) else {
            dao.update(entity)
            account.id
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    suspend fun insertAll(accounts: List<Account>) =
        dao.insertAll(accounts.map { it.toEntity() })

    suspend fun deleteAll() = dao.deleteAll()

    private fun AccountEntity.toModel() = Account(
        id = id,
        name = name,
        type = type,
        initialBalance = initialBalance,
        color = color,
        icon = icon,
        isArchived = isArchived,
    )

    private fun Account.toEntity() = AccountEntity(
        id = id,
        name = name,
        type = type,
        initialBalance = initialBalance,
        color = color,
        icon = icon,
        isArchived = isArchived,
    )
}
