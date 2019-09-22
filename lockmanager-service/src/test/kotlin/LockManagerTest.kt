import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LockManagerTest {

    @Test
    fun `try to acquire balance update lock with its not present`() {
        callPrivate(LockMangerService, "clearLockCollection")
        assertTrue(LockMangerService.acquireLock("1234"), "able to acquire lock")
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
        var isLocked = false
        GlobalScope.launch {
           isLocked =  LockMangerService.acquireLock("1234", 2000)
        }
        runBlocking { delay(100) }
        LockMangerService.releaseLock("1234")
        runBlocking { delay(2) }
        assertTrue(isLocked)
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

    @Test
    fun `release lock which was never aquired`() {
        callPrivate(LockMangerService, "clearLockCollection")
        assertThrows(LockReleaseException::class.java) {
            LockMangerService.releaseLock("1234")
        }
    }


    @Test
    fun `release same lock twice`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireLock("1234")
        LockMangerService.releaseLock("1234")
        assertThrows(LockReleaseException::class.java) {
            LockMangerService.releaseLock("1234")
        }
    }
}


fun callPrivate(obj: Any, methodName: String, value: Any? = null) {
    if (value != null) {
        val method = obj::class.java.getDeclaredMethod(methodName, value::class.java)
        method.isAccessible = true
        method.invoke(obj, value)
    } else {
        val method = obj::class.java.getDeclaredMethod(methodName)
        method.isAccessible = true
        method.invoke(obj)
    }
}


