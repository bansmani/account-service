import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

object TransactionStatuService {

    val transactionIdEventList = ConcurrentHashMap<String, TransactionStatusDTO>()

    fun getTransactionStatus(transactionId: String): TransactionStatus {
        return CrudRepsitory.queryById(transactionId, Transaction::class.java)?.status ?: TransactionStatus.UNKNOWN
    }

    //add timeout
    fun getTransactionStatusBlocking(transactionId: String): TransactionStatusDTO? {
        while (!transactionIdEventList.contains(transactionId)) {
            runBlocking {
                delay(100)
            }
        }
        return transactionIdEventList[transactionId]
    }

    fun transactionStatusPoller() {
        DomainEventManager.startTransactionStatusMessageListener {
            //raise events on Observable transactions to avoid BD events
            CrudRepsitory.updateFields(Transaction::class.java, it)
            transactionIdEventList.put(it.transactionId, it)
        }
    }
}