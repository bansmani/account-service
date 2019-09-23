import io.mockk.Ordering
import io.mockk.mockkObject
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Instant


class TransactionServiceTest {


    companion object {

        private fun cleanActiveMQTempDir() {
            File("../").walkTopDown()
                .filter { file -> file.name == "activemq-data" }
                .forEach { println(it.deleteRecursively()) }
        }

        @BeforeAll
        @JvmStatic
        fun setup() {
            cleanActiveMQTempDir()
            TransactionService.start()
            DebitService.start()
            CreditService.start()
        }


    }


    @Test
    fun `transaction service generates valid transaction on every instruction payload received`() {
        //When a DEBIT Instruction is received
        val creditInstructionPayload = InstructionDTO(1234, 5000.0, InstructionType.CREDIT, "Tuition Fees")
        TransactionService.createNewTransaction(creditInstructionPayload)

        val transaction: Transaction = TransactionService.createNewTransaction(
            InstructionDTO(1234, 500.0, InstructionType.DEBIT, "Tuition Fees paid")
        )
        //then it should be valid
        assertEquals(InstructionType.DEBIT.name, transaction.instructionType.name)
        assertEquals(TransactionStatus.NEW.name, transaction.status.name)
        assertThat(transaction.transactionId).isNotNull().isNotBlank().isNotEmpty()
        assertThat(transaction.initiateTime).isNotNull().isInstanceOf(Instant::class.java)
        assertThat(transaction.endTime).isNull()

        //When a CREDIT Instruction is received
        val anotherTransaction: Transaction = TransactionService.createNewTransaction(
            InstructionDTO(1234, 500.0, InstructionType.CREDIT, "Tuition Fees")
        )
        assertEquals(InstructionType.CREDIT.name, anotherTransaction.instructionType.name)
    }


    @Test
    fun `transaction should be routed according to payloads instruction type`() {
        mockkObject(DomainEventManager)

        val creditInstructionPayload = InstructionDTO(1234, 500.0, InstructionType.CREDIT, "Tuition Fees")
        TransactionService.createNewTransaction(creditInstructionPayload)
        verify { DomainEventManager.publishCredit(ofType(AccountEntry::class)) }

        Thread.sleep(1000)

        val debitInstructionPayload = InstructionDTO(1234, 500.0, InstructionType.DEBIT, "Tuition Fees")
        TransactionService.createNewTransaction(debitInstructionPayload)
        verify { DomainEventManager.publishDebit(ofType(AccountEntry::class)) }


    }

    @Test
    fun `transaction should be persist and available to query`() {
        mockkObject(CrudRepsitory)
        val credit = InstructionDTO(1234, 500.0, InstructionType.CREDIT, "Tuition Fees")
        TransactionService.createNewTransaction(credit)
        verify(atLeast = 1) { CrudRepsitory.save(ofType(Transaction::class), true) }
    }

    //TODO: revisit, seems like verify Order/sequence does not work as expected
    //TODO: raise issue on git hub
    // @Test
    fun `test sequence of events`() {
        mockkObject(DomainEventManager)
        mockkObject(CrudRepsitory)
        val debitInstructionPayload = InstructionDTO(1234, 500.0, InstructionType.DEBIT, "Tuition Fees")
        TransactionService.createNewTransaction(debitInstructionPayload)
        verifyOrder {
            CrudRepsitory.save(any())
            DomainEventManager.publishDebit(ofType(AccountEntry::class))
        }
        verify(Ordering.ORDERED, atLeast = 1) {
            CrudRepsitory.save(any())
            DomainEventManager.publishCredit(ofType(AccountEntry::class))
        }
    }



    @Test
    fun `local transfer test with insufficient funds`() {
        //TODO: should throw custom exception
        assertThrows(Exception::class.java, {
            TransactionService.localTransfer(
                LocalTransferDTO(8888, 5678, 250.0, "transfer A to B"))
        }, "Insufficient funds")
    }

    @Test
    fun `local transfer test with positive balance`() {
        val creditInstruction = TestDomainModelFactory().buildCreditInstructionDto(333)
        TransactionService.createNewTransaction(creditInstruction)
        TransactionService.localTransfer(LocalTransferDTO(333, 1111, 200.0, "transfer A to B"))
        assertEquals(200.0, BalanceService.getBalance(1111))
        assertEquals(300.0, BalanceService.getBalance(333))
    }


}


