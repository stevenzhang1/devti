package cc.unitmesh.processor.api.model.postman

import kotlinx.serialization.Serializable

@Serializable
class PostmanScript {
    var id: String? = null
    var type: String? = null
    var exec: List<String>? = null
}
