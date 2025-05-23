package org.http4k.core

import org.http4k.routing.asRouter

/**
 * See https://www.rfc-editor.org/rfc/rfc9110.html#name-accept
 */
data class Accept(val contentTypes: List<QualifiedContent>) {

    /**
     * Note that the Accept header ignores CharSet because that is in a separate Accept-CharSet header..
     */
    fun accepts(contentType: ContentType): Boolean =
        contentTypes.any { it.content.equalsIgnoringDirectives(contentType) }
}

data class QualifiedContent(val content: ContentType, val priority: Double = 1.0)

fun ContentType.accepted() = { req: Request ->
    (req.headerValues("Accept").filterNotNull()
        .flatMap { it.split(", ") })
        .map { it.substringBefore(";").trim() }
        .any { it == withNoDirectives().toHeaderValue() }
}.asRouter("Accepts $this")
