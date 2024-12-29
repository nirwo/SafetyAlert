package nir.wolff.model

data class ChatMessage(
    val id: String = "",
    val groupId: String = "",
    val senderEmail: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "groupId" to groupId,
        "senderEmail" to senderEmail,
        "message" to message,
        "timestamp" to timestamp
    )
}
