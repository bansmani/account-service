import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class BalanceServiceTest {


    @Test
    fun `when updatebalance not previous balance was available`() {

        // val balanceCache = BalanceCache(1234, 100.0, Instant.now(), "first entry")
        assertEquals(0.0, BalanceService.getBalance(1234))

        val entry = TestDomainModelFactory().buildCreditEntry(500.0)

        BalanceService.updateBalance(entry)

        assertEquals(500.0, BalanceService.getBalance(entry.accNumber))

    }
}