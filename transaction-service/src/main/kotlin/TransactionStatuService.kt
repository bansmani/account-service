import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.util.concurrent.ConcurrentHashMap

object TransactionStatuService {

    val transactionIdEventList = ConcurrentHashMap<String, TransactionStatusDTO>()

    fun getTransactionStatus(transactionId: String): TransactionStatus {
        return CrudRepsitory.queryById(transactionId, Transaction::class.java)?.status ?: TransactionStatus.UNKNOWN
    }

    //add timeout
    fun getTransactionStatusBlocking(transactionId: String): TransactionStatusDTO? {
        while (!transactionIdEventList.containsKey(transactionId)) {
            runBlocking {
//                delay(1)
                yield()
            }
        }
        val dto = transactionIdEventList[transactionId]
        transactionIdEventList.remove(transactionId)
        return dto
    }

    fun transactionStatusPoller() {
        DomainEventManager.startTransactionStatusMessageListener {
            //raise events on Observable transactions to avoid BD events
            CrudRepsitory.updateFields(Transaction::class.java, it)
            transactionIdEventList[it.transactionId] = it
        }
    }
}