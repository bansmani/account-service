import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

object LockMangerService : IlockManagerService {

    private val lockCollection = HashSet<Lock>()

    override fun releaseLock(lock: Lock): Boolean {
        if (!lockCollection.remove(lock)) throw LockReleaseException("Lock already Expired")
        return true
    }

    override fun acquireDebitTransactionLock(lockName: String, timeout: Long): Lock {
        val lock = Lock(lockName, LockType.DEBIT_ACCOUNT_ENTRY_LOCK)
        return tryAquireLock(lock, timeout)
    }

    //Mutually Exclusive keep longer timeout
    //caller must retry on exception
    override fun acquireBalanceUpdateLock(lockName: String, timeout: Long): Lock {
        val lock = Lock(lockName, LockType.BALANCE_UPDATE_LOCK)
        return tryAquireLock(lock, timeout)
    }

    private fun tryAquireLock(lock:Lock, timeout: Long): Lock {
        var counter = timeout + 1
        while (counter-- > 0) {
            if (lockCollection.add(lock)) {
                return lock
            } else {
                runBlocking {
                    delay(1)
                }
            }
        }
        throw LockAquireFailed(lock.lockName)
    }

    private fun clearLockCollection() {
        lockCollection.clear()
    }

}

class LockReleaseException(s: String) : Throwable(s)
class LockAquireFailed(lockName: String) : Throwable(lockName)


interface IlockManagerService {
    fun acquireDebitTransactionLock(lockName: String, timeout: Long): Lock
    fun acquireBalanceUpdateLock(lockName: String, timeout: Long = 0): Lock
    fun releaseLock(lock: Lock): Boolean
}

enum class LockType {
    BALANCE_UPDATE_LOCK,
    DEBIT_ACCOUNT_ENTRY_LOCK
}

data class Lock(val lockName: String, val lockType: LockType) {
    //lockType
    //makLockDuration
    //lockCreationTime
    //lockExpiryTime
}
