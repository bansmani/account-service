import TransactionStatuService.transactionStatusPoller
import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.function.Executable
import java.io.File
import java.lang.Exception
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger


class TransactionServiceTest {


    companion object {
        val auth = mockk<Authentication>()

        fun cleanActiveMQTempDir() {
            File("../").walkTopDown()
                .filter { file -> file.name == "activemq-data" }
                .forEach { println(it.deleteRecursively()) }
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            every { auth.verify(any()) } returns true
            cleanActiveMQTempDir()
            DebitService.start()
            CreditService.start()
            transactionStatusPoller()
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
        assertEquals(InstructionType.DEBIT.name, transaction.instructionType!!.name)
        assertEquals(TransactionStatus.NEW.name, transaction.status.name)
        assertThat(transaction.transactionId).isNotNull().isNotBlank().isNotEmpty()
        assertThat(transaction.initiateTime).isNotNull().isInstanceOf(Instant::class.java)
        assertThat(transaction.endTime).isNull()

        //When a CREDIT Instruction is received
        val anotherTransaction: Transaction = TransactionService.createNewTransaction(
            InstructionDTO(1234, 500.0, InstructionType.CREDIT, "Tuition Fees")
        )
        assertEquals(InstructionType.CREDIT.name, anotherTransaction.instructionType!!.name)
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

        //first create transaction object
        //save to database
        //publish payload
        //return transaction to user
    }

    //invalid since listener is running
    //Integration Test
    //@Test
    fun `transaction service sending debit messages to debit queue`() {
        val debitInstructionPayload = TestDomainModelFactory().buildDebitInstructionDto()
        TransactionService.createNewTransaction(debitInstructionPayload)
        var entry: AccountEntry? = null
        DomainEventManager.startDebitMessageLister {
            entry = it
        }
        while (entry == null) {
            Thread.sleep(10)
        }
        assertEquals(debitInstructionPayload.accNumber, entry?.accNumber)
        assertEquals(InstructionType.DEBIT, entry?.transactionType)
        assertEquals(300.0, entry?.amount)
    }

    //invalid since listener is running
    //@Test
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
        assertEquals(creditInstruction.accNumber, entry?.accNumber)
        assertEquals(InstructionType.CREDIT, entry?.transactionType)
        assertEquals(500.0, entry?.amount)
    }

    @Test
    fun `local transfer test with insufficient funds`() {
        //TODO: should throw custom exception
        assertThrows(Exception::class.java, Executable {
            TransactionService.localTransfer(LocalTransferDTO(1234, 5678, 250.0, "transfer A to B"))
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


    @Test
    fun `create mutliple credit and check eventual consitance`() {
        (1..100).forEach {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(9876, 10.0))
            //      TransactionService.localTransfer(LocalTransferDTO(9876, 1111, 200.0, "transfer A to B"))
            //    assertEquals(200.0, BalanceService.getBalance(1111))
//            assertEquals(300.0, BalanceService.getBalance(9876))
        }
        Thread.sleep(1000)
        assertEquals(1000.0, BalanceService.getBalance(9876))
    }


    @Test
    fun `create mutliple debit and check strong consitance`() {
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(4444, 1000.0))
        while (BalanceService.getBalance(4444) != 1000.0) {
            Thread.sleep(10)
        }
        var expectedBalance = 1000.0
        (1..100).forEach {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildDebitInstructionDto(4444, 10.0))
            //      TransactionService.localTransfer(LocalTransferDTO(9876, 1111, 200.0, "transfer A to B"))
            //    assertEquals(200.0, BalanceService.getBalance(1111))
//            assertEquals(300.0, BalanceService.getBalance(9876))
            expectedBalance = BigDecimal(expectedBalance).subtract(BigDecimal(10.0)).toDouble()
            assertEquals(expectedBalance, BalanceService.getBalance(4444))
        }
       // Thread.sleep(1000)
            // assertEquals(1000.0, BalanceService.getBalance(9876))
    }

    @Test
    fun `concurrency deadloack test with single account`(){
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(2222, 2000.0))
        while (BalanceService.getBalance(2222) != 2000.0) {
            Thread.sleep(10)
        }
        (1..100).forEach {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildDebitInstructionDto(2222, 10.0))
            TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(2222, 10.0))
        }
        Thread.sleep(2000)
        assertEquals(2000.0, BalanceService.getBalance(2222))
    }
}


