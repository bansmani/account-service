import io.javalin.Javalin
import io.javalin.http.Context
import java.time.Instant


object RestController {
    fun startServer() {
        TransactionService.start()

        val app = Javalin.create {
            it.asyncRequestTimeout = 1000 * 120
        }.start(7000)

        app.get("/health") { ctx -> health(ctx) }
        app.post("/transfer") { ctx -> localTransfer(ctx) }
        app.exception(InsufficientFundsException::class.java) { exception, ctx ->
            ctx.json(buildExceptionResponse(exception, ctx.body()))
        }
    }

    fun health(ctx: Context): Context {
        return ctx.result("OK")
    }

    fun localTransfer(ctx: Context): Context {
        val localTransferDTO: LocalTransferDTO = ctx.bodyAsClass(LocalTransferDTO::class.java)

        val transaction = TransactionService.localTransfer(localTransferDTO)

        if (transaction?.status == TransactionStatus.ERROR) {
            ctx.status(500)
        }
        if (transaction?.status == TransactionStatus.FAILED) {
            ctx.status(501)
        }
        return ctx.json(transaction!!)
    }

    fun buildExceptionResponse(e: Exception, payload: Any) : ExcceptionResponse {
       return ExcceptionResponse(e.javaClass.name, e.message, Instant.now().toString(), payload)
    }
}

data class ExcceptionResponse(val errorCode: String,
                          val message: String?,
                          val time:String,
                          val requestPayload: Any)
