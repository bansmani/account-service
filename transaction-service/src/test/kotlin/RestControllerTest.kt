import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import io.javalin.http.Context
import io.mockk.mockkObject
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File

class RestControllerTest {


    companion object {


        private fun cleanActiveMQTempDir() {
            File("../").walkTopDown()
                .filter { file -> file.name == "activemq-data" }
                .forEach { println(it.deleteRecursively()) }
        }

        @BeforeAll
        @JvmStatic
        fun setup() {
            cleanActiveMQTempDir()
            DebitService.start()
            CreditService.start()
            RestController.startServer()
        }

    }


    @Test
    fun `server runing on port 7000 and health is ok`() {
        val status = "$baseUrl/health".httpGet().responseString().third.get()
        assertEquals("OK", status)
    }

    @Test
    fun `exception response is handled`() {
        val atoB = LocalTransferInstructionDTO(6666, 5555, 150.0, "transfer A to B")
        mockkObject(RestController)
        val output = "/transfer".postDTO(atoB, ExcceptionResponse::class.java)
        assertEquals("InsufficientFundsException", output.errorCode)
        verify(exactly = 1) { RestController.localTransfer(ofType(Context::class)) }
    }


    @Test
    fun `transfer fund locally REST integration test`() {
        //create initial balance
        val creditInstructionPayload = InstructionDTO(2121, 5000.0, InstructionType.CREDIT, "Tuition Fees")
        TransactionService.createNewTransaction(creditInstructionPayload)

        val atoB = LocalTransferInstructionDTO(2121, 1212, 150.0, "transfer A to B")
        val output = "/transfer".postDTO(atoB, Transaction::class.java)
        val transactionStatus = TransactionStatuService.getTransactionStatus(output.transactionId)
        assertEquals("COMPLETED", transactionStatus?.status?.name)
    }

}



const val baseUrl = "http://localhost:7000"
val gson = Gson()
fun String.postDTO(dto: Any): String {
    return Fuel.post("$baseUrl$this")
        .timeout(120 * 1000)
        .timeoutRead(120 * 1000)
        .jsonBody(gson.toJson(dto)).responseString().third.get()
}

fun <T> String.postDTO(dto: Any, responseType: Class<T>): T {
    val string = this.postDTO(dto)
    return gson.fromJson(string, responseType)
}

