import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.jupiter.api.*
import java.lang.Exception
import java.time.Instant

class DebitServiceTest {


    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            cleanActiveMQTempDir()
        }
    }

    @BeforeEach
    fun beforeEach() {
        unmockkAll()
        JMSMessageBroker.clearQueue("TRANSACTION.STATUS")

        CrudRepsitory.execute("truncate table AccountEntry", true)
        CrudRepsitory.execute("truncate table Transaction", true)
        CrudRepsitory.execute("truncate table BalanceCache", true)
    }

    @Test
    fun `raise error event on insufficient funds `() {
        val accountEntry = AccountEntry(
            TestDomainModelFactory().generateAccNumber(),
            500.0, Instant.now(), "test-txn-1", InstructionType.DEBIT
        )
        assertThrows<InsufficientFundsException> {
            DebitService.processDebit(accountEntry)
        }
    }

    @Test
    fun `raise completed event in sufficient funds is available `() {
        buildInitialCache(12345, 500.0)
        //make some deposits
        CrudRepsitory.save(
            AccountEntry(
                12345,
                1000.0,
                Instant.now(),
                "test-txn-2",
                InstructionType.CREDIT
            ), true
        )
        //refresh caches manually
        BalanceService.refreshCacheWithNoLock(12345)

        //attempt a debit
        val accountEntry = AccountEntry(
            12345,
            500.0, Instant.now(), "test-txn-3", InstructionType.DEBIT
        )
        DebitService.processDebit(accountEntry)

        val txtnStatus = DomainEventManager.pollTransactionStatus(1000)
        Assertions.assertEquals("COMPLETED", txtnStatus.status.name)
    }

    @Test
    fun `can widthraw all money`() {
        buildInitialCache(4444, 500.0)
        val accountEntry = AccountEntry(
            4444,
            500.0, Instant.now(), "test-txn-123", InstructionType.DEBIT
        )
        DebitService.processDebit(accountEntry)
        val txtnStatus = DomainEventManager.pollTransactionStatus(1000)
        Assertions.assertEquals("test-txn-123", txtnStatus.transactionId)
        Assertions.assertEquals("COMPLETED", txtnStatus.status.name)
    }

    @Test //sort of duplicate
    fun `can not widthraw more than available balance money`() {
        buildInitialCache(6677, 500.0)
        //make some deposits
        val accountEntry = AccountEntry(
            6677,
            600.0, Instant.now(), "test-txn-1111", InstructionType.DEBIT
        )

        assertThrows<InsufficientFundsException> {
            DebitService.processDebit(accountEntry)
        }
    }


    @Test
    fun `create a compensating credit transaction if balance cache is not updated`() {
        buildInitialCache(6688, 500.0)
        //add some credit
        CrudRepsitory.save(
            AccountEntry(
                6688,
                1000.0,
                Instant.now(),
                "test-txn-244",
                InstructionType.CREDIT
            ), true
        )
        BalanceService.refreshCache(6688)

        mockkObject(BalanceService)
        every { BalanceService.updateBalanceWithNoLock(any()) }.throws(Exception("something went wrong"))
        //attempt a debit
        val accountEntry = AccountEntry(
            6688,
            300.0, Instant.now(), "test-txn-3123", InstructionType.DEBIT
        )
        //debit now
        DebitService.processDebit(accountEntry)

        val balance = BalanceService.getBalance(accountEntry.accNumber)
        Assertions.assertEquals(1000.0,  balance)
        val txtnStatus = DomainEventManager.pollTransactionStatus(1000)
        Assertions.assertEquals("test-txn-3123", txtnStatus.transactionId)
        Assertions.assertEquals("ERROR", txtnStatus.status.name)

    }


    private fun buildInitialCache(accNumber: Long, amount: Double = 500.0) {
        CrudRepsitory.save(
            BalanceCache(
                accNumber,
                amount,
                Instant.now(),
                "txn1234"
            ), true
        )
    }


}