import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception
import java.time.Instant

class DebitServiceTest {

    @Test
    fun `raise error event on insufficient funds `() {
        val accountEntry = AccountEntry(
            TestDomainModelFactory().generateAccNumber(),
            500.0, Instant.now(), "test-txn-1", InstructionType.DEBIT
        )
        assertThrows<InsufficientFundsException> {
            DebitService.processDebit(accountEntry)
        }
//        val pollTransactionStatus = DomainEventManager.pollTransactionStatus(1000)
//        println(pollTransactionStatus)
    }

    @Test
    fun `raise completed in sufficient funds is available `() {
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
        println(txtnStatus)
        Assertions.assertEquals("COMPLETED", txtnStatus.status.name)
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