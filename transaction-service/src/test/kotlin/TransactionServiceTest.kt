import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant


class TransactionServiceTest {


    companion object {
        val auth = mockk<Authentication>()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            every { auth.verify(any()) } returns true
        }
    }


    @Test
    fun `transaction service generates valid transaction on every instruction payload received`() {
        //When a DEBIT Instruction is received
        val transaction: Transaction = TransactionService.createNewTransaction(
            InstructionDTO(1234, 500.0, InstructionType.DEBIT, "Tuition Fees paid")
        )
        //then it should be valid
        assertEquals(InstructionType.DEBIT.name, transaction.instructionType!!.name)
        assertEquals(TransactionStatus.NEW.name, transaction.status.name)
        assertThat(transaction.id).isNotNull().isNotBlank().isNotEmpty()
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
        val debitInstructionPayload = InstructionDTO(1234, 500.0, InstructionType.DEBIT, "Tuition Fees")
        TransactionService.createNewTransaction(debitInstructionPayload)
        verify { DomainEventManager.publishDebit(ofType(AccountEntry::class)) }

        val creditInstructionPayload = InstructionDTO(1234, 500.0, InstructionType.CREDIT, "Tuition Fees")
        TransactionService.createNewTransaction(creditInstructionPayload)
        verify { DomainEventManager.publishCredit(ofType(AccountEntry::class)) }

    }

    @Test
    fun `transaction should be persist and available to query`() {
        mockkObject(CrudRepsitory)
        val debitInstructionPayload = InstructionDTO(1234, 500.0, InstructionType.DEBIT, "Tuition Fees")
        TransactionService.createNewTransaction(debitInstructionPayload)
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

    //Integration Test
    @Test
    fun `transaction service sending debit messages to debit queue`() {
        val debitInstructionPayload = TestDomainModelFactory().buildDebitInstructionDto()
        TransactionService.createNewTransaction(debitInstructionPayload)
        var entry: AccountEntry? =null
        DomainEventManager.startDebitMessageLister {
            entry = it
        }
        while (entry==null){
            Thread.sleep(10)
        }
        assertEquals(debitInstructionPayload.accNumber, entry?.accNumber)
        assertEquals(InstructionType.DEBIT, entry?.transactionType)
        assertEquals(300.0, entry?.amount)
    }

    @Test
    fun `transaction service sending credit messages to credit queue`() {
        val creditInstruction = TestDomainModelFactory().buildCreditInstructionDto()
        TransactionService.createNewTransaction(creditInstruction)
        var entry: AccountEntry? =null
        DomainEventManager.startCreditMessageLister {
            entry = it
        }
        while (entry==null){
            Thread.sleep(10)
        }
        assertEquals(creditInstruction.accNumber, entry?.accNumber)
        assertEquals(InstructionType.CREDIT, entry?.transactionType)
        assertEquals(500.0, entry?.amount)
    }
}


