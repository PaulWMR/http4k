package org.http4k.ai.langchain4j.chat

import dev.forkhandles.result4k.map
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.ContentType
import dev.langchain4j.data.message.ImageContent
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.TextContent
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ChatRequest
import dev.langchain4j.model.chat.response.ChatResponse
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role.Companion.Assistant
import org.http4k.ai.model.Role.Companion.System
import org.http4k.ai.model.Role.Companion.User
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.ollama.Message
import org.http4k.connect.ollama.Ollama
import org.http4k.connect.ollama.ResponseFormat
import org.http4k.connect.ollama.action.ModelOptions
import org.http4k.connect.ollama.chatCompletion
import org.http4k.connect.orThrow

fun OllamaChatLanguageModel(
    ollama: Ollama,
    model: ModelName,
    stream: Boolean? = null,
    format: ResponseFormat? = null,
    keep_alive: String? = null,
    options: ModelOptions? = null,
) = object : ChatModel {
    override fun doChat(chatRequest: ChatRequest) = with(chatRequest) {
        ollama.chatCompletion(model, messages().map { it.toHttp4k() }, stream, format, keep_alive, options)
            .map {
                ChatResponse.builder()
                    .aiMessage(AiMessage(it.mapNotNull { it.message?.content }.joinToString("")))
                    .build()
            }
            .orThrow()
    }
}

private fun ChatMessage.toHttp4k(): Message = when (this) {
    is UserMessage -> toHttp4k()
    is AiMessage -> toHttp4k()
    is SystemMessage -> toHttp4k()
    else -> error("unknown message type")
}

private fun UserMessage.toHttp4k(): Message {
    val text = contents().filter { it.type() == ContentType.TEXT }
        .takeIf { it.size == 1 }
        ?.first()?.let { (it as TextContent).text() }
        ?: error("Only single text is supported")

    val images = contents().filter { it.type() == ContentType.IMAGE }
        .map { Base64Blob.of((it as ImageContent).image().base64Data()) }
    return Message(User, text, images)
}

private fun AiMessage.toHttp4k() = Message(Assistant, text())
private fun SystemMessage.toHttp4k() = Message(System, text())
