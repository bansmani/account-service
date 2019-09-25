import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
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
                CrudRepsitory.updateFields(Transaction::class.java, it)
                transactionIdEventList[it.transactionId] = it
            }
        }
    }
}