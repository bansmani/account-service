import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LockManagerTest {

    @Test
    fun `try to acquire balance update lock with its not present`() {
        callPrivate(LockMangerService, "clearLockCollection")
        val lock = LockMangerService.acquireBalanceUpdateLock("1234")
        assertEquals("1234", lock.lockName)
    }


    @Test
    fun `try to acquire balance update lock when its already locked`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireBalanceUpdateLock("1234")
        assertThrows(LockAquireFailed::class.java) {
            LockMangerService.acquireBalanceUpdateLock("1234")
        }
    }

    @Test
    fun `try to acquire balance update lock when its already locked with timeout in range`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireBalanceUpdateLock("1234")
        var lock: Lock? = null
        GlobalScope.launch {
            lock = LockMangerService.acquireBalanceUpdateLock("1234", 2000)
        }
        runBlocking { delay(100) }
        callPrivate(LockMangerService, "clearLockCollection")
        runBlocking { delay(2) }
        assertEquals("1234", lock?.lockName)
    }


    @Test
    fun `try to acquire balance update lock when its already locked with timeout hit`() {
        callPrivate(LockMangerService, "clearLockCollection")
        LockMangerService.acquireBalanceUpdateLock("1234")
        assertThrows(LockAquireFailed::class.java) {
            LockMangerService.acquireBalanceUpdateLock("1234",100)
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


