import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class BalanceServiceTest {



    @Test
    fun `when not balance information available`(){
        assertEquals(0.0, BalanceService.getBalance(1234))
    }

    @Test
    fun `when update balance not previous balance was available`() {
        CrudRepsitory.execute("truncate table BalanceCache")
        assertEquals(0.0, BalanceService.getBalance(1234))
        BalanceService.updateBalance(TestDomainModelFactory().buildCreditEntry(accNumber = 1234))
        assertEquals(500.0, BalanceService.getBalance(1234))
        BalanceService.updateBalance(TestDomainModelFactory().buildCreditEntry(accNumber = 1234))
        assertEquals(1000.0, BalanceService.getBalance(1234))
        BalanceService.updateBalance(TestDomainModelFactory().buildCreditEntry(100.50, 1234))
        assertEquals(1100.50, BalanceService.getBalance(1234))
    }

    @Test
    fun `when I call balance update it acquire lock and release lock on completion`(){
        mockkObject(LockMangerService)
        BalanceService.updateBalance(TestDomainModelFactory().buildCreditEntry(accNumber = 1234))
        verify(exactly = 1) { LockMangerService.acquireLock(any(), any())}
        verify(exactly = 1) { LockMangerService.releaseLock(any())}
    }



}