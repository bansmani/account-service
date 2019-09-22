import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.annotations.TestOnly

//could use google distributed lock algorithm
object LockMangerService : IlockManagerService {


    //TODO: Redis could used for distributed locks (RedLock)
    private val lockCollection = HashSet<String>()

    override fun releaseLock(lockName: String) {
        lockCollection.remove(lockName)
    }

    override fun acquireLock(lockName: String, timeout: Long) {
        var counter = timeout + 1
        while (counter-- > 0) {
            if (lockCollection.add(lockName)) {
                return
            } else {
                runBlocking {
                    delay(1)
                }
            }
        }
        throw LockAquireFailed(lockName)
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
    fun acquireLock(lockName: String, timeout: Long = 0)
    fun releaseLock(lockName: String)
}


//proposed for TTL lock including automatic expiry
//only lock owner and LockMangerAdmin can release locks
//data class Lock(val lockName: String) {
//  lockType (autoexpire lock)
//  LockDuration
//  lockCreationTime
//  lockExpiryTime
//  LockOwner
//}
