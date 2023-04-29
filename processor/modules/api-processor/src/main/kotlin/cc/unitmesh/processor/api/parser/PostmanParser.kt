package cc.unitmesh.processor.api.parser

import cc.unitmesh.processor.api.base.ApiDetail
import cc.unitmesh.processor.api.base.BodyMode
import cc.unitmesh.processor.api.base.Parameter
import cc.unitmesh.processor.api.base.Request
import cc.unitmesh.processor.api.base.Response
import cc.unitmesh.processor.api.model.postman.PostmanCollection
import cc.unitmesh.processor.api.model.postman.PostmanEnvironment
import cc.unitmesh.processor.api.model.postman.PostmanFolder
import cc.unitmesh.processor.api.model.postman.PostmanItem
import cc.unitmesh.processor.api.model.postman.PostmanUrl
import cc.unitmesh.processor.api.model.postman.PostmanVariables

class PostmanParser {
    private val `var`: PostmanVariables = PostmanVariables(PostmanEnvironment())
    fun parse(collection: PostmanCollection): List<List<ApiDetail>>? {
        return collection.item?.map {
            parseFolder(it, it.name)
        }
    }

    private fun parseFolder(item: PostmanFolder, folderName: String?): List<ApiDetail> {
        val details: MutableList<ApiDetail> = mutableListOf()
        if (item.item != null) {
            for (subItem in item.item!!) {
                parseItem(subItem, folderName, item.name).let {
                    details.addAll(it)
                }
            }
        }

        return details.toList()
    }

    private fun parseItem(subItem: PostmanItem, folderName: String?, itemName: String?): List<ApiDetail> {
        if (subItem.item != null) {
            return subItem.item.map {
                parseItem(it, folderName, itemName)
            }.flatten() ?: listOf()
        }

        if (subItem.request != null) {
            return listOf(formatOutput(subItem, folderName, itemName))
        }

        return listOf()
    }

    private fun formatOutput(
        subItem: PostmanItem,
        folderName: String?,
        itemName: String?
    ): ApiDetail {
        val request = subItem.request
        val url: PostmanUrl? = request?.url
        val method = request?.method
        val body = request?.body
        val description = request?.description
        val name = subItem.name

        var uri = request?.getUrl(`var`)
        if (uri?.startsWith("UNDEFINED") == true) {
            uri = uri.substring(9)
        }

        val responses = subItem.response?.map {
            Response(
                code = it.code ?: 0,
                parameters = listOf(),
                bodyMode = BodyMode.RAW_TEXT,
                bodyString = it.body ?: "",
            )
        }?.toList() ?: listOf()

        val req = Request(
            parameters = urlParameters(url),
            body = body?.formdata?.map { Parameter(it.key ?: "", it.value ?: "") } ?: listOf(),
        )

        return ApiDetail(
            method = method ?: "",
            path = uri ?: "",
            description = description?.replace("\n", " ") ?: "",
            operationId = name ?: "",
            tags = listOf(folderName ?: "", itemName ?: ""),
            request = req,
            response = responses,
        )
    }

    private fun urlParameters(url: PostmanUrl?): List<Parameter> {
        val variable = url?.variable?.map {
            Parameter(it.key ?: "", it.value ?: "")
        }

        val queries = url?.query?.map {
            Parameter(it.key ?: "", it.value ?: "")
        }

        return (variable ?: listOf()) + (queries ?: listOf())
    }
}

