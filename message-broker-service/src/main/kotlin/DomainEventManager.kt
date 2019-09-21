import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import org.objenesis.ObjenesisStd

val gson = Gson()

object DomainEventManager {
    private val deserializer = accountEntryDeserializer();

    fun publishDebit(payload: AccountEntry) {
        val jsonString = gson.toJson(payload)
        JMSMessageBroker.send("TRANSACTION.DEBIT", jsonString)
    }

    fun publishCredit(payload: AccountEntry) {
        val jsonString = gson.toJson(payload)
        JMSMessageBroker.send("TRANSACTION.CREDIT", jsonString)
    }


    fun startDebitMessageLister(handler: (message: AccountEntry) -> Unit) {
        startMessageListener("TRANSACTION.DEBIT", handler)
    }

    fun startCreditMessageLister(handler: (message: AccountEntry) -> Unit) {
        startMessageListener("TRANSACTION.CREDIT", handler)

    }

    private fun startMessageListener(queueName : String, handler: (message: AccountEntry) -> Unit) {
        JMSMessageBroker.startConsumer(queueName) {
            val message = deserializer.fromJson(it, AccountEntry::class.java)
            handler(message)
        }
    }

    private fun accountEntryDeserializer() =
        GsonBuilder().registerTypeAdapter(AccountEntry::class.java,
            InstanceCreator<AccountEntry> { ObjenesisStd().newInstance(AccountEntry::class.java) }).create()


}
