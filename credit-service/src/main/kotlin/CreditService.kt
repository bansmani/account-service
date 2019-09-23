import DomainEventManager.updateTransactionStatus

object CreditService {

    fun start() {
        DomainEventManager.startCreditMessageLister {
            process(it)
        }
    }

    //must give credit otherwise logs
    private fun process(entry: AccountEntry) {
        try {
            retry { CrudRepsitory.save(entry) }
        } catch (e: Exception) {
            updateTransactionStatus(TransactionStatusDTO(entry.transactionId, TransactionStatus.FAILED, e.message?: ""))
        }
        try {
            retry { BalanceService.updateBalance(entry) }
        } catch (e: Exception) {
            //Not very critical, no compensating transaction required
            updateTransactionStatus(TransactionStatusDTO(entry.transactionId, TransactionStatus.ERROR,e.message?: ""))
        }
        updateTransactionStatus(TransactionStatusDTO(entry.transactionId, TransactionStatus.COMPLETED,""))
    }
}

//should move to utility functions
fun <T> retry(retrycount: Int = 3, method: () -> T): T {
    var count = retrycount - 1
    while (count-- > 0) {
        try {
            return method()
        } catch (e: Exception) {
        }
    }
    return method()
}


fun main() {
    CreditService.start()
}