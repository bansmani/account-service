import java.math.BigDecimal
import java.time.Instant
import java.util.*

object BalanceService : IBalanceService {

    override fun updateBalance(accountEntry: AccountEntry): Boolean {
        //should be a REST call, but testing it should be fine

        val lock = acquireBalanceUpdateLock(accountEntry.accNumber)
        updateBalanceWithNoLock(accountEntry)
        LockMangerService.releaseLock(lock)
        return true
    }

    override fun updateBalanceWithNoLock(accountEntry: AccountEntry): Boolean {
        val balanceCache: BalanceCache? = CrudRepsitory.queryById(accountEntry.accNumber, BalanceCache::class.java)
        if (balanceCache != null) {
            val currentBalance = BigDecimal(balanceCache.balanceAmount)
            val newBalance = if (accountEntry.transactionType == InstructionType.DEBIT) {
                currentBalance.subtract(BigDecimal(accountEntry.amount)).toDouble()
            } else {
                currentBalance.add(BigDecimal(accountEntry.amount)).toDouble()
            }
            CrudRepsitory.update(
                BalanceCache(accountEntry.accNumber, newBalance, Instant.now(), accountEntry.transactionId)
            )
        } else {
            if (accountEntry.transactionType == InstructionType.DEBIT) {
                //TODO: the question is Neg balance allowed
                CrudRepsitory.save(
                    BalanceCache(
                        accountEntry.accNumber,
                        -accountEntry.amount,
                        Instant.now(),
                        accountEntry.transactionId
                    )
                )
            } else {
                CrudRepsitory.save(
                    BalanceCache(
                        accountEntry.accNumber, accountEntry.amount, Instant.now(), accountEntry.transactionId
                    ), true
                )
            }
        }
        return true
    }


    fun acquireBalanceUpdateLock(accNumber: Long): String {
        val lock = "${accNumber}_UPDATE_BALANCE_LOCK"
        LockMangerService.acquireLock(lock, 5000)
        return lock
    }

    override fun getBalance(accNumber: Long): Double {
        val queryById = CrudRepsitory.queryById(accNumber, BalanceCache::class.java)
        return queryById?.balanceAmount ?: 0.0
    }


}



interface IBalanceService {
    fun updateBalance(accountEntry: AccountEntry): Boolean
    fun updateBalanceWithNoLock(accountEntry: AccountEntry): Boolean
    fun getBalance(accNumber: Long): Double
}

class BalanceCache(@Id val accNumber: Long, val balanceAmount: Double, val updateTime: Instant, val updatedRef: String)


//just for logging
class BalanceCacheUpdateLog(
    val accNumber: Long, val balance: Double, val updateTime: Instant, val updatedRef: String
) {
    @Id
    val updateId: String = UUID.randomUUID().toString()
}