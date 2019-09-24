import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.objenesis.ObjenesisStd
import java.io.File
import java.time.Instant
import kotlin.system.measureTimeMillis


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


    @BeforeEach
    fun clearMock() {
        unmockkAll()
    }

    @Test
    fun `transaction service generates valid transaction on every instruction payload received`() {
        //When a DEBIT Instruction is received
        val transaction = createInitialBalance()

        Thread.sleep(100)
        assertEquals(InstructionType.CREDIT.name, transaction.instructionType.name)
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
        createInitialBalance()
        verify { DomainEventManager.publishCredit(ofType(AccountEntry::class)) }
        Thread.sleep(100)
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


    @Test
    fun `local transfer test with insufficient funds`() {
        //TODO: should throw custom exception
        assertThrows(Exception::class.java, {
            TransactionService.localTransfer(
                LocalTransferInstructionDTO(8888, 5678, 250.0, "transfer A to B")
            )?.get()
        }, "Insufficient funds")
    }

    @Test
    fun `local transfer test with positive balance`() {
        val creditInstruction = TestDomainModelFactory().buildCreditInstructionDto(333)
        TransactionService.createNewTransaction(creditInstruction)

        TransactionService.localTransfer(
            LocalTransferInstructionDTO(
                333,
                1111,
                200.0,
                "transfer A to B"
            )
        )?.get()
        Thread.sleep(100)

        assertEquals(200.0, BalanceService.getBalance(1111))
        assertEquals(300.0, BalanceService.getBalance(333))
    }


    @Test
    fun `transfer money locally should not block to other transactions if not completed`() {
        //transaction service should be non blocking.
        mockkObject(TransactionService)
        mockkObject(TransactionStatuService)

        every { TransactionStatuService.getTransactionStatus(any()) }.returns(ObjenesisStd().newInstance(Transaction::class.java))

        every { runBlocking { TransactionService.createNewTransaction(any()) } }.answers { call ->
            Thread.sleep(1000); ObjenesisStd().newInstance(Transaction::class.java)
        }
        val timetook = measureTimeMillis {
            //mutiple transfer
            TransactionService.localTransfer(LocalTransferInstructionDTO(333, 1111, 200.0, "transfer A to B"))
            TransactionService.localTransfer(LocalTransferInstructionDTO(222, 1111, 200.0, "transfer A to B"))
            TransactionService.localTransfer(LocalTransferInstructionDTO(111, 1111, 200.0, "transfer A to B"))
        }
        println(timetook)
        assertFalse(timetook > 200, "should not wait for another transaction")
    }


    @Test
    fun `I should be able to create transaction for B even when transaction for A is not completed`() {
        //transaction service should be non blocking.
        mockkObject(TransactionService)

        mockkObject(TransactionStatuService)
        every { TransactionStatuService.getTransactionStatus(any()) }.returns(ObjenesisStd().newInstance(Transaction::class.java))

        every { runBlocking { TransactionService.createNewTransaction(any()) } }.answers { call ->
            Thread.sleep(1000); ObjenesisStd().newInstance(Transaction::class.java)
        }
        val mrA =
            TransactionService
                .localTransfer(LocalTransferInstructionDTO(333, 1111, 200.0, "transfer A to B"))

        every { runBlocking { TransactionService.createNewTransaction(any()) } }.answers { call ->
            Thread.sleep(0); ObjenesisStd().newInstance(Transaction::class.java)
        }

        val mrB =
            TransactionService
                .localTransfer(LocalTransferInstructionDTO(222, 1111, 200.0, "transfer A to B"))

        Thread.sleep(10)
        assertFalse(mrA!!.isDone)
        assertTrue(mrB!!.isDone)
    }




    private fun createInitialBalance(accNumber: Long = 1234, amount: Double = 500.0): Transaction {
        val creditInstructionPayload = InstructionDTO(accNumber, amount, InstructionType.CREDIT, "Tuition Fees")
        return TransactionService.createNewTransaction(creditInstructionPayload)
    }
}


