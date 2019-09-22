import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import org.objenesis.ObjenesisStd

val gson = Gson()

object DomainEventManager {
    private val deserializer = accountEntryDeserializer();
    private val txnStatusDeserializer = transactionStatusDeserializer();

    fun updateTransactionStatus(payload: TransactionStatusDTO){
        val jsonString = gson.toJson(payload)
        JMSMessageBroker.send("TRANSACTION.STATUS", jsonString)
    }

    fun publishDebit(payload: AccountEntry) {
        val jsonString = gson.toJson(payload)
        JMSMessageBroker.send("TRANSACTION.DEBIT", jsonString)
    }

    fun publishCredit(payload: AccountEntry) {
        val jsonString = gson.toJson(payload)
        JMSMessageBroker.send("TRANSACTION.CREDIT", jsonString)
    }


    fun startDebitMessageLister(handler: (message: AccountEntry) -> Unit) {
        startAccountEntryMessageListener("TRANSACTION.DEBIT", handler)
    }

    fun startCreditMessageLister(handler: (message: AccountEntry) -> Unit) {
        startAccountEntryMessageListener("TRANSACTION.CREDIT", handler)

    }

    fun startTransactionStatusMessageListener(handler: (message: TransactionStatusDTO) -> Unit) {
        JMSMessageBroker.startConsumer("TRANSACTION.STATUS") {
            val message = txnStatusDeserializer.fromJson(it, TransactionStatusDTO::class.java)
            handler(message)
        }
    }

    private fun startAccountEntryMessageListener(queueName : String, handler: (message: AccountEntry) -> Unit) {
        JMSMessageBroker.startConsumer(queueName) {
            val message = deserializer.fromJson(it, AccountEntry::class.java)
            handler(message)
        }
    }

    private fun accountEntryDeserializer() =
        GsonBuilder().registerTypeAdapter(AccountEntry::class.java,
            InstanceCreator<AccountEntry> { ObjenesisStd().newInstance(AccountEntry::class.java) }).create()

    private fun transactionStatusDeserializer() =
        GsonBuilder().registerTypeAdapter(TransactionStatusDTO::class.java,
            InstanceCreator<TransactionStatusDTO> { ObjenesisStd().newInstance(TransactionStatusDTO::class.java) }).create()
}
