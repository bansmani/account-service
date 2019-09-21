import javafx.beans.NamedArg
import java.lang.annotation.Native
import java.time.Instant
import java.util.*
import javax.management.MXBean

data class InstructionDTO(
    val accNumber: Long,
    val amount: Double,
    val instructionType: InstructionType,
    val description: String? = null
)

enum class InstructionType {
    DEBIT,
    CREDIT
}

class Transaction(
    val instructionType: InstructionType,
    val accNumber: Long, val amount: Double, val description: String? = ""
) {
    val id = UUID.randomUUID().toString()
    val initiateTime: Instant = Instant.now()
    val endTime: Instant? = null
    val status: TransactionStatus = TransactionStatus.NEW

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Transaction
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "Transaction(transactionType=$instructionType, tansactionId='$id', transactionStartTime=$initiateTime, transactionEndTime=$endTime, transactionStatus=$status)"
    }
}





data class AccountEntry(
    val accNumber: Long,
    val amount: Double,
    val transactionTime: Instant,
    val transactionId: String,
    val transactionType: InstructionType,
    val description: String? = null
)


enum class TransactionStatus {
    NEW,
    COMPLETED,
    FAILED,
    ERROR
}


