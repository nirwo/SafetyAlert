package nir.wolff.model

import com.google.firebase.firestore.DocumentSnapshot

data class Group(
    val id: String = "",
    val name: String = "",
    val adminEmail: String = "",
    val members: List<GroupMember> = emptyList()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "adminEmail" to adminEmail,
            "members" to members.map { it.toMap() }
        )
    }

    companion object {
        fun fromDocument(document: DocumentSnapshot): Group {
            val data = document.data ?: return Group()
            return Group(
                id = document.id,
                name = data["name"] as? String ?: "",
                adminEmail = data["adminEmail"] as? String ?: "",
                members = (data["members"] as? List<Map<String, Any>>)?.map { 
                    GroupMember.fromMap(it)
                } ?: emptyList()
            )
        }
    }
}
