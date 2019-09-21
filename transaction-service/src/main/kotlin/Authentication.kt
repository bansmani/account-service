interface Authentication {
    fun verify(token: String): Boolean
}