import java.math.BigDecimal
import java.time.Instant
import java.util.*

object BalanceService : IBalanceService {
    override fun updateBalance(accountEntry: AccountEntry): Boolean {
        //should be a REST call
        val acquireBalanceUpdateLock: Lock =
            LockMangerService.acquireBalanceUpdateLock(accountEntry.accNumber.toString(), 5000)
        val balanceCache: BalanceCache? = CrudRepsitory.queryById(accountEntry.accNumber, BalanceCache::class.java)

        if (balanceCache != null) {
            var newBalance = 0.0
            val currentBalance = BigDecimal(balanceCache.balanceAmount)
            if (accountEntry.transactionType == InstructionType.DEBIT) {
                newBalance = currentBalance.subtract(BigDecimal(accountEntry.amount)).toDouble()
            } else {
                newBalance = currentBalance.add(BigDecimal(accountEntry.amount)).toDouble()
            }
            CrudRepsitory.saveOrUpdate(
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

    override fun getBalance(accNumber: Long): Double {
        val queryById = CrudRepsitory.queryById(accNumber, BalanceCache::class.java)
        return queryById?.balanceAmount ?: 0.0
    }


}

//blocking service
interface IBalanceService {
    fun updateBalance(accountEntry: AccountEntry): Boolean
    fun getBalance(accNumber: Long): Double
}

class BalanceCache(@Id val accNumber: Long, val balanceAmount: Double, val updateTime: Instant, val updatedRef: String)

class BalanceCacheUpdateLog(
    val accNumber: Long, val balance: Double, val updateTime: Instant, val updatedRef: String
) {
    @Id
    val updateId: String = UUID.randomUUID().toString()
}