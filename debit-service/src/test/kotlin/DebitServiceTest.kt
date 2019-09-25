import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.Instant

class DebitServiceTest {

    @Test
    fun `raise error event on insufficient funds `() {
        val accountEntry = AccountEntry(
            TestDomainModelFactory().generateAccNumber(),
            500.0, Instant.now(), "test-txn-1", InstructionType.DEBIT
        )
        callPrivate(DebitService, "process", accountEntry)
        val pollTransactionStatus = DomainEventManager.pollTransactionStatus(1000)
        println(pollTransactionStatus)
    }

    @Test
    fun `raise completed in sufficient funds `() {

        CrudRepsitory.save(
            BalanceCache(
                12345,
                500.0,
                Instant.now(),
                "txn1234"
            ), true
        )

        CrudRepsitory.save(
            AccountEntry(
                12345,
                1000.0,
                Instant.now(),
                "test-txn-2",
                InstructionType.CREDIT
            ), true
        )
  CrudRepsitory.save(
            AccountEntry(
                12345,
                140.0,
                Instant.now(),
                "test-txn-4",
                InstructionType.CREDIT
            ), true
        )


        BalanceService.refreshCacheWithNoLock(12345)

        val accountEntry = AccountEntry(12345,
            500.0, Instant.now(), "test-txn-3", InstructionType.DEBIT
        )
        callPrivate(DebitService, "process", accountEntry)
        val pollTransactionStatus = DomainEventManager.pollTransactionStatus(1000)
        println(pollTransactionStatus)
    }


}