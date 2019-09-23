import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal

class IntegrationTest {
    companion object {

        fun cleanActiveMQTempDir() {
            File("../").walkTopDown()
                .filter { file -> file.name == "activemq-data" }
                .forEach { println(it.deleteRecursively()) }
        }

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            cleanActiveMQTempDir()
            startServices()
        }

        fun startServices() {
            //Note: still running in same same JVM
            //to run them on different JVM only need to update JNDI property
            //Sequence of service does not matter

            //horizontally scalable design, can run multiple instance
            TransactionService.start()
            DebitService.start()
            TransactionService.start()
            DebitService.start()
            TransactionService.start()
            CreditService.start()
            TransactionService.start()
            DebitService.start()
            DebitService.start()
            CreditService.start()
            CreditService.start()
            CreditService.start()
        }
    }


    @Test
    fun `create mutliple credit and check eventual consitance`() {
        (1..100).forEach {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(9876, 10.0))
        }
        Thread.sleep(1000)
        Assertions.assertEquals(1000.0, BalanceService.getBalance(9876))
    }


    @Test
    fun `create mutliple debit and check strong consitance`() {
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(4444, 1000.0))
        while (BalanceService.getBalance(4444) != 1000.0) {
            Thread.sleep(10)
        }
        var expectedBalance = 1000.0
        (1..100).forEach {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildDebitInstructionDto(4444, 10.0))

            expectedBalance = BigDecimal(expectedBalance).subtract(BigDecimal(10.0)).toDouble()
            Assertions.assertEquals(expectedBalance, BalanceService.getBalance(4444))
        }
        // Thread.sleep(1000)
        // assertEquals(1000.0, BalanceService.getBalance(9876))
    }

    @Test
    fun `concurrency deadloack test with single account`() {
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(2222, 2000.0))
        while (BalanceService.getBalance(2222) != 2000.0) {
            Thread.sleep(10)
        }
        (1..100).forEach {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildDebitInstructionDto(2222, 10.0))
            TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(2222, 10.0))
        }
        Thread.sleep(2000)
        Assertions.assertEquals(2000.0, BalanceService.getBalance(2222))
    }

    @Test
    fun `Deadlock check, A sending moeny to B and same time B sending back to A`() {
        //Given
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(6666, 2000.0))
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(5555, 1000.0))

        val AtoB = LocalTransferDTO(6666, 5555, 150.0, "transfer A to B")
        val BtoA = LocalTransferDTO(5555, 6666, 100.0, "transfer B to A")

        (1..10).forEach {
            TransactionService.localTransfer(AtoB)
            TransactionService.localTransfer(BtoA)
        }

        val Abal = 2000.0 - 1500.0 + 1000.0
        val Bbal = 1000.0 - 1000.0 + 1500.0
        val AplusB = 1000.0 + 2000.0

        Thread.sleep(1000)

       assertEquals(1500.0,  BalanceService.getBalance(6666))
       assertEquals(1500.0,  BalanceService.getBalance(5555))

    }

}