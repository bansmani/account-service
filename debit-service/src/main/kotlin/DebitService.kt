import DomainEventManager.updateTransactionStatus
import java.lang.Math.abs
import java.math.BigDecimal

object DebitService {

    fun start() {
        DomainEventManager.startDebitMessageLister {
            process(it)
        }
    }

    private fun process(entry: AccountEntry) {
        try {
            LockMangerService.acquireLock(entry.accNumber.toString() + "_DEBIT_ENTRY_LOCK", 2000)
        } catch (e: Exception) {
            updateTransactionStatus(
                TransactionStatusDTO(
                    entry.transactionId,
                    TransactionStatus.FAILED,
                    e.message ?: ""
                )
            )
            return
        }

        val balanceUpdateLock = try {
            BalanceService.acquireBalanceUpdateLock(entry.accNumber)
        } catch (e: Exception) {
            LockMangerService.releaseLock(entry.accNumber.toString() + "_DEBIT_ENTRY_LOCK")
            updateTransactionStatus(
                TransactionStatusDTO(
                    entry.transactionId,
                    TransactionStatus.FAILED,
                    e.message ?: ""
                )
            )
            return
        }

        try {
            //Assuming : Neg balance is not allowed
            val balance: Double = BalanceService.getBalance(entry.accNumber)
            val amount = BigDecimal(entry.amount)

            //if neg balance limit is give, check against the limit and not the zero
            if (BigDecimal(balance).subtract(amount).toDouble() < 0.0) {
                updateTransactionStatus(TransactionStatusDTO(entry.transactionId, TransactionStatus.ERROR,  "Insufficient funds"))
                LockMangerService.releaseLock(balanceUpdateLock)
                LockMangerService.releaseLock(entry.accNumber.toString() + "_DEBIT_ENTRY_LOCK")
                return
            }
            retry { CrudRepsitory.save(entry) }
        } catch (e: Exception) {
            LockMangerService.releaseLock(balanceUpdateLock)
            LockMangerService.releaseLock(entry.accNumber.toString() + "_DEBIT_ENTRY_LOCK")
            updateTransactionStatus(
                TransactionStatusDTO(
                    entry.transactionId,
                    TransactionStatus.FAILED,
                    e.message ?: ""
                )
            )
            return
        }
        try {
            retry { BalanceService.updateBalanceWithNoLock(entry) }
        } catch (e: Exception) {
            //compensate debit with a credit
            AccountEntry(
                entry.accNumber,
                entry.amount,
                entry.transactionTime,
                entry.transactionId,
                InstructionType.CREDIT,
                "reversing due to error : " + e.message + " " + entry.description
            )
            retry { CrudRepsitory.save(entry) }
            updateTransactionStatus(TransactionStatusDTO(entry.transactionId, TransactionStatus.ERROR, e.message ?: ""))
            return
        } finally {
            LockMangerService.releaseLock(balanceUpdateLock)
            LockMangerService.releaseLock(entry.accNumber.toString() + "_DEBIT_ENTRY_LOCK")
        }
        updateTransactionStatus(TransactionStatusDTO(entry.transactionId, TransactionStatus.COMPLETED, ""))
    }
}

//should move to utility functions
fun <T> retry(retrycount: Int = 3, method: () -> T): T {
    var count = retrycount - 1
    while (count-- > 0) {
        try {
            return method()
        } catch (e: Exception) {
            //log error with trace
            e.printStackTrace()
        }
    }
    return method()
}


fun main() {
    DebitService.start()
}