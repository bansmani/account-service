import javax.jms.*
import javax.naming.InitialContext


object JMSMessageBroker : MessageBroker {
    private val context = InitialContext()
    private val queueConnectionFactory = context.lookup("ConnectionFactory") as QueueConnectionFactory
    private val queueConnection = queueConnectionFactory.createQueueConnection()
    private val session = queueConnection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE)

    private val producers = mutableMapOf<String, MessageProducer>()


    init {
        queueConnection.start()
    }

    override fun send(queueName: String, message: String) {
        val producer = producers[queueName] ?: session.createProducer(session.createQueue(queueName))
        producers[queueName] = producer
        producer.send(session.createTextMessage(message) as Message)
    }

    override fun startConsumer(queueName: String, handler: (msg: String) -> Unit) {
        val creditQueue = session.createQueue(queueName)
        val consumer = session.createConsumer(creditQueue)

        consumer.setMessageListener {
            handler((it as TextMessage).text)
        }

    }

    override fun pollMessage(queueName: String, timeoutInMillis: Long): String {
        val creditQueue = session.createQueue(queueName)
        val consumer = session.createConsumer(creditQueue)
        return (consumer.receive(timeoutInMillis) as TextMessage).text
    }

}

interface MessageBroker {
    fun send(queueName: String, message: String)
    fun startConsumer(queueName: String, handler: (msg: String) -> Unit)
    fun pollMessage(queueName: String, timeoutInMillis: Long) : String
}
