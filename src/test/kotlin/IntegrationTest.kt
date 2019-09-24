import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture

class IntegrationTest {
    companion object {

        private fun cleanActiveMQTempDir() {
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

        private fun startServices() {
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
        repeat((1..100).count()) {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(9876, 10.0))
        }
        Thread.sleep(1000)
        assertEquals(1000.0, BalanceService.getBalance(9876))
    }


    @Test
    fun `create mutliple debit and check strong consitance`() {
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(4444, 1000.0))
        while (BalanceService.getBalance(4444) != 1000.0) {
            Thread.sleep(10)
        }
        var expectedBalance = 1000.0
        repeat((1..100).count()) {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildDebitInstructionDto(4444, 10.0))
            expectedBalance = BigDecimal(expectedBalance).subtract(BigDecimal(10.0)).toDouble()
            assertEquals(expectedBalance, BalanceService.getBalance(4444))
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
        repeat((1..100).count()) {
            TransactionService.createNewTransaction(TestDomainModelFactory().buildDebitInstructionDto(2222, 10.0))
            TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(2222, 10.0))
        }
        Thread.sleep(2000)
        assertEquals(2000.0, BalanceService.getBalance(2222))
    }

    @Test
    fun `Deadlock check, A sending moeny to B and same time B sending back to A`() {
        //Given
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(6666, 2000.0))
        TransactionService.createNewTransaction(TestDomainModelFactory().buildCreditInstructionDto(5555, 1000.0))


        val atoB = LocalTransferInstructionDTO(6666, 5555, 150.0, "transfer A to B")
        val btoA = LocalTransferInstructionDTO(5555, 6666, 100.0, "transfer B to A")

        //when
        val futureList = mutableListOf<CompletableFuture<Transaction>?>()
        repeat((1..10).count()) {
            futureList.add(TransactionService.localTransfer(atoB))
            futureList.add(TransactionService.localTransfer(btoA))
        }

        //wait for credits to process
        futureList.forEach { it?.get() }
        Thread.sleep(100)

        assertEquals(1500.0, BalanceService.getBalance(6666))
        assertEquals(1500.0, BalanceService.getBalance(5555))
    }

}