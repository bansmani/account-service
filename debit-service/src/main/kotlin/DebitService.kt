import DomainEventManager.updateTransactionStatus
import java.math.BigDecimal
import java.time.Instant

object DebitService {

    fun start() {
        DomainEventManager.startDebitMessageLister {
            try {
                BalanceService.acquireAccountEntryLock(it.accNumber)
                processDebit(it)
            } catch (e: Exception) {
                updateTransactionStatus(
                    TransactionStatusDTO(
                        it.transactionId,
                        TransactionStatus.FAILED,
                        e.message ?: "",
                        Instant.now().toString()
                    )
                )
            } finally {
                BalanceService.releaseAccountEntryLock(it.accNumber)
            }
        }
    }


    fun processDebit(entry: AccountEntry) {
        //Stage 1
        makeAccountEntry(entry)

        //Stage 2
        try {
            retry { BalanceService.updateBalanceWithNoLock(entry) }
        } catch (e: Exception) {
            //compensate debit with a credit on stage 2 failure
            retry { CrudRepsitory.save(entry.getCompensatingEntry()) }
            updateTransactionStatus(
                TransactionStatusDTO(
                    entry.transactionId,
                    TransactionStatus.ERROR,
                    e.message ?: "",
                    Instant.now().toString()
                )
            )
            return
        }

        //Stage 3 notify success
        retry {
            updateTransactionStatus(
                TransactionStatusDTO(
                    entry.transactionId,
                    TransactionStatus.COMPLETED,
                    "",
                    Instant.now().toString()
                )
            )
        }
    }

    private fun makeAccountEntry(entry: AccountEntry) {
        //Assuming : Neg balance is not allowed
        //if neg balance is allowed and limit is given, check against the limit and not the zero
        val balance: Double = BalanceService.getBalance(entry.accNumber)
        val amount = BigDecimal(entry.amount)
        if (BigDecimal(balance).subtract(amount).toDouble() < 0.0) {
            throw InsufficientFundsException(
                "Insufficient funds for account : ${entry.accNumber}, " +
                        "current balance is : $balance, " +
                        "transaction account is $amount"
            )
        }
        retry { CrudRepsitory.save(entry) }
    }
}

fun main() {
    DebitService.start()
}