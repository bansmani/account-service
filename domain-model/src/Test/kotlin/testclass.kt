//import io.javalin.Javalin
//import kotlinx.coroutines.CompletableDeferred
//import kotlinx.coroutines.async
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.runBlocking
//import java.time.Instant
//import java.util.concurrent.CompletableFuture
//import kotlin.system.measureTimeMillis
//
//fun main(args: Array<String>) {
//
////    val app = Javalin.create().start(7000)
//
////    app.get("/") { ctx -> ctx.json("") }
//
//calculate()
//}
//
//fun calculate() {
//
//     CompletableFuture.supplyAsync {
//        println(Instant.now().toString())
//        println("starting debit")
//        Thread.sleep(1000)
//        println("completed debit")
//        "debit"
//    }.thenApply {
//        println(Instant.now().toString())
//        println("starting credit")
//        Thread.sleep(1000)
//        println("completed credit")
//        "$it  : credit"
//    }.thenApply {
//        "$it + completed"
//    }
//
//   // Thread.sleep(3000)
//
//}
