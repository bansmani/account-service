import org.jetbrains.annotations.TestOnly

fun <T> retry(retrycount: Int = 3, method: () -> T): T {
    var count = retrycount - 1
    while (count-- > 0) {
        try {
            return method()
        } catch (e: Exception) {
            //log error with trace
            e.printStackTrace()
        }
    }
    return method()
}

@TestOnly
fun callPrivate(obj: Any, methodName: String, value: Any? = null) {
    if (value != null) {
        val method = obj::class.java.getDeclaredMethod(methodName, value::class.java)
        method.isAccessible = true
        method.invoke(obj, value)
    } else {
        val method = obj::class.java.getDeclaredMethod(methodName)
        method.isAccessible = true
        method.invoke(obj)
    }
}