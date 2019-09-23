import DomainEventManager.updateTransactionStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant


//Integraton tests which are breaking due to running listerns in same JVM
//this message can be processed by other listers (AMQP or Kafka could solve this issue with consumer groups)
//no more valid since listener is running

class IntegrationTests{

    @Test
    fun `transaction service sending debit messages to debit queue`() {
        TransactionService.start()

        var entry: AccountEntry? = null
        DomainEventManager.startDebitMessageLister {
            entry = it
            updateTransactionStatus(TransactionStatusDTO(it.transactionId, TransactionStatus.COMPLETED, "", Instant.now().toString()))
        }
        val debitInstructionPayload = TestDomainModelFactory().buildDebitInstructionDto()
        TransactionService.createNewTransaction(debitInstructionPayload)

        while (entry == null) {
            Thread.sleep(10)
        }
        Assertions.assertEquals(debitInstructionPayload.accNumber, entry?.accNumber)
        Assertions.assertEquals(InstructionType.DEBIT, entry?.transactionType)
        Assertions.assertEquals(300.0, entry?.amount)
    }


    @Test
    fun `transaction service sending credit messages to credit queue`() {
        val creditInstruction = TestDomainModelFactory().buildCreditInstructionDto()
        TransactionService.createNewTransaction(creditInstruction)
        var entry: AccountEntry? = null
        DomainEventManager.startCreditMessageLister {
            entry = it
        }
        while (entry == null) {
            Thread.sleep(10)
        }
        Assertions.assertEquals(creditInstruction.accNumber, entry?.accNumber)
        Assertions.assertEquals(InstructionType.CREDIT, entry?.transactionType)
        Assertions.assertEquals(500.0, entry?.amount)
    }
}