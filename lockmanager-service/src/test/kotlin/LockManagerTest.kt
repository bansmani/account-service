import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LockManagerTest {

    @Test
    fun `try to acquire balance update lock with its not present`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireLock("1234")
        assertTrue(true, "able to acquire lock")
    }

    @Test
    fun `try to acquire balance update lock when its already locked`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireLock("1234")
        assertThrows(LockAquireFailed::class.java) {
            LockMangerService.acquireLock("1234")
        }
    }

    @Test
    fun `try to acquire balance update lock when its already locked with timeout in range`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireLock("1234")
        GlobalScope.launch {
            LockMangerService.acquireLock("1234", 2000)
        }
        runBlocking { delay(100) }
        LockMangerService.releaseLock("1234")
        runBlocking { delay(2) }
        assertTrue(true)
    }


    @Test
    fun `try to acquire balance update lock when its already locked and hit timeout`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireLock("1234")
        assertThrows(LockAquireFailed::class.java) {
            LockMangerService.acquireLock("1234", 100)
        }
    }

    @Test
    fun `release and aquire lock`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireLock("1234")
        assertThrows(LockAquireFailed::class.java) {
            LockMangerService.acquireLock("1234")
        }
        LockMangerService.releaseLock("1234")
        LockMangerService.acquireLock("1234")
    }


}




