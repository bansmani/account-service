import DomainEventManager.updateTransactionStatus
import java.math.BigDecimal
import java.time.Instant

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
                    e.message ?: "", Instant.now().toString()
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
                    e.message ?: "", Instant.now().toString()
                )
            )
            return
        }

        try {
            //Assuming : Neg balance is not allowed
            val balance: Double = BalanceService.getBalance(entry.accNumber)
            val amount = BigDecimal(entry.amount)

            //if neg balance is allowed and limit is given, check against the limit and not the zero
            if (BigDecimal(balance).subtract(amount).toDouble() < 0.0) {

                updateTransactionStatus(
                    TransactionStatusDTO(
                        entry.transactionId,
                        TransactionStatus.ERROR,
                        "Insufficient funds",Instant.now().toString()
                    )
                )
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
                    e.message ?: "", Instant.now().toString()
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
            updateTransactionStatus(TransactionStatusDTO(entry.transactionId, TransactionStatus.ERROR, e.message ?: "", Instant.now().toString()))
            return
        } finally {
            LockMangerService.releaseLock(balanceUpdateLock)
            LockMangerService.releaseLock(entry.accNumber.toString() + "_DEBIT_ENTRY_LOCK")
        }
        updateTransactionStatus(TransactionStatusDTO(entry.transactionId, TransactionStatus.COMPLETED, "", Instant.now().toString()))
    }
}

//should move to utility functions



fun main() {
    DebitService.start()
}