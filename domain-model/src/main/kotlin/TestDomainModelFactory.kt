import org.jetbrains.annotations.TestOnly
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAmount
import java.util.*


val testDomainFactory = TestDomainModelFactory()

//Testonly is not working as expected, better to create a module and add them as testCompile to all project
class TestDomainModelFactory @TestOnly constructor() {

    fun generateAccNumber(): Long {
        Thread.sleep(1)
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddhhmmSSS")).toLong()
    }

    fun buildCreditInstructionDto(accNumber: Long = generateAccNumber(), amount: Double=500.0) =
        InstructionDTO(accNumber, amount, InstructionType.CREDIT, "Tuition Fees")

    fun buildDebitInstructionDto(accNumber: Long = generateAccNumber(), amount: Double=300.0) =
        InstructionDTO(accNumber, amount, InstructionType.DEBIT, "Tuition Fees")

    fun buildCreditEntry(amount: Double=500.0, accNumber: Long = generateAccNumber()) = AccountEntry(accNumber,amount, Instant.now(),
            UUID.randomUUID().toString(),InstructionType.CREDIT,"Test Credit message")

    fun buildDebitEntry(amount: Double=300.0, accNumber: Long = generateAccNumber()) = AccountEntry(accNumber,amount, Instant.now(),
        UUID.randomUUID().toString(),InstructionType.DEBIT,"Test Debit message")

    //fun buildlocalTransferDTO(amount: Double, accNumber: Long) =


}