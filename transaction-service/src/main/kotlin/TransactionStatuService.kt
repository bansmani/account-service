import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

object TransactionStatuService {

    private val transactionIdEventList = ConcurrentHashMap<String, TransactionStatusDTO>()

    fun getTransactionStatus(transactionId: String): Transaction? {
        return CrudRepsitory.queryById(transactionId, Transaction::class.java)
    }

    //add timeout
    fun getTransactionStatusBlocking(transactionId: String): TransactionStatusDTO? {
        while (!transactionIdEventList.containsKey(transactionId)) {
            runBlocking {
                delay(1)
                yield()
            }
        }
        val dto = transactionIdEventList[transactionId]
        transactionIdEventList.remove(transactionId)
        return dto
    }

    fun transactionStatusPoller() {
        GlobalScope.launch {


            DomainEventManager.startTransactionStatusMessageListener {
                //raise events on Observable transactions to avoid BD events

                CrudRepsitory.updateFields(Transaction::class.java, it)

                transactionIdEventList[it.transactionId] = it
            }
        }
    }
}