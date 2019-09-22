import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.Test

class CreditServiceTest {


    @Test
    fun `When receive credit entry it should save in database and retrun true`(){
            mockkObject(CrudRepsitory)
            CreditService.start()
            DomainEventManager.publishCredit(testDomainFactory.buildCreditEntry())
            verify(atLeast = 1){CrudRepsitory.save(ofType(AccountEntry::class))}
    }

//    @Test
//    fun `When receive credit entry it should try to acquire lock before database save`(){
//        mockkObject(CrudRepsitory)
//        mockkObject(LockMangerService)
//        CreditService.start()
//        DomainEventManager.publishCredit(testDomainFactory.buildCreditEntry())
//        verify(atLeast = 1){LockMangerService.acquireBalanceUpdateLock(any())}
//        verify(atLeast = 1){CrudRepsitory.save(ofType(AccountEntry::class))}
//    }
}