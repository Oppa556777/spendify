package com.myexpense.tracker.data.repository

import com.myexpense.tracker.data.database.dao.BillSplitDao
import com.myexpense.tracker.data.database.dao.BillSplitMemberDao
import com.myexpense.tracker.data.database.dao.PersonDao
import com.myexpense.tracker.data.database.entity.BillSplitEntity
import com.myexpense.tracker.data.database.entity.BillSplitMemberEntity
import com.myexpense.tracker.utils.toMinorUnits
import com.myexpense.tracker.utils.toRupees
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class SplitMemberView(
    val id: Long,
    val personId: Long,
    val personName: String,
    val avatarColor: Long,
    val share: Long,          // minor units
    val isPaid: Boolean,
)

data class BillSplitView(
    val id: Long,
    val title: String,
    val total: Long,          // minor units
    val date: Long,
    val note: String?,
    val paidByPersonId: Long?,
    val payerName: String?,
    val members: List<SplitMemberView>,
) {
    val paidTotal: Long get() = members.filter { it.isPaid }.sumOf { it.share }
    val allSettled: Boolean get() = members.isNotEmpty() && members.all { it.isPaid }
}

@Singleton
class BillSplitRepository @Inject constructor(
    private val dao: BillSplitDao,
    private val memberDao: BillSplitMemberDao,
    private val personDao: PersonDao,
) {

    fun observeAll(): Flow<List<BillSplitView>> =
        combine(dao.observeAll(), memberDao.observeAll(), personDao.observeAll()) { splits, members, people ->
            val personMap = people.associateBy { it.id }
            splits.map { split ->
                val splitMembers = members.filter { it.splitId == split.id }.map { m ->
                    val person = personMap[m.personId]
                    SplitMemberView(
                        id = m.id,
                        personId = m.personId,
                        personName = person?.name ?: "Person",
                        avatarColor = person?.avatarColor?.let { color ->
                            runCatching { color.toLong(16) or 0xFF000000 }.getOrDefault(0xFF3B82F6)
                        } ?: 0xFF3B82F6,
                        share = m.shareAmount.toMinorUnits(),
                        isPaid = m.isPaid,
                    )
                }
                BillSplitView(
                    id = split.id,
                    title = split.title,
                    total = split.totalAmount.toMinorUnits(),
                    date = split.date,
                    note = split.note,
                    paidByPersonId = split.paidByPersonId,
                    payerName = split.paidByPersonId?.let { personMap[it]?.name },
                    members = splitMembers,
                )
            }
        }

    /** Creates a split + its members in a transaction. */
    suspend fun saveSplit(
        title: String,
        totalMinor: Long,
        date: Long,
        note: String?,
        paidByPersonId: Long?,
        members: List<Triple<Long?, String, Long>>,  // (personId?, name, shareMinor)
    ): Long {
        // resolve inline names into people
        val resolved = members.map { (personId, name, share) ->
            val id = personId ?: personDao.insert(
                com.myexpense.tracker.data.database.entity.PersonEntity(
                    name = name,
                    avatarColor = String.format("#%06X", 0x3B82F6),
                )
            )
            id to share
        }
        val splitId = dao.insert(
            BillSplitEntity(
                title = title,
                totalAmount = totalMinor.toRupees(),
                date = date,
                note = note,
                paidByPersonId = paidByPersonId,
            )
        )
        memberDao.insertAll(
            resolved.map { (personId, share) ->
                BillSplitMemberEntity(splitId = splitId, personId = personId, shareAmount = share.toRupees())
            }
        )
        return splitId
    }

    suspend fun togglePaid(memberId: Long, paid: Boolean) {
        memberDao.getById(memberId)?.let { memberDao.update(it.copy(isPaid = paid)) }
    }

    suspend fun deleteSplit(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() {
        dao.deleteAll()
        memberDao.deleteAll()
    }
}
