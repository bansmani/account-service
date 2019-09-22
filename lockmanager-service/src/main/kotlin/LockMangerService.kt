import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly

object LockMangerService : IlockManagerService {


    //TODO: Redis could used for distributed locks (RedLock)
    private val lockCollection = HashSet<String>()

    override fun releaseLock(lock: String): Boolean {
        if (!lockCollection.remove(lock)) throw LockReleaseException("Lock already Expired")
        return true
    }

    override fun acquireLock(lock: String, timeout: Long): Boolean {
        var counter = timeout + 1
        while (counter-- > 0) {
            if (lockCollection.add(lock)) {
                return true
            } else {
                runBlocking {
                    delay(1)
                }
            }
        }
        throw LockAquireFailed(lock)
    }

    //for testing, should not go in production
    @TestOnly
    private fun clearLockCollection() {
        lockCollection.clear()
    }

}

class LockReleaseException(lockName: String) : Throwable(lockName)
class LockAquireFailed(lockName: String) : Throwable(lockName)


interface IlockManagerService {
    fun acquireLock(lockName: String, timeout: Long = 0): Boolean
    fun releaseLock(lockName: String): Boolean
}


//proposed for TTL lock including automatic expiry
//data class Lock(val lockName: String) {
//  lockType (autoexpire lock)
//  LockDuration
//  lockCreationTime
//  lockExpiryTime
//  LockOwner
//}
