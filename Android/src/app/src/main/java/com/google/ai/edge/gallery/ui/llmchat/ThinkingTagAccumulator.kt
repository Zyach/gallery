package com.google.ai.edge.gallery.ui.llmchat

private const val OPEN_THINK_TAG = "<think>"
private const val CLOSE_THINK_TAG = "</think>"

data class ThinkingParseChunk(
  val text: String = "",
  val thinking: String = "",
)

class ThinkingTagAccumulator {
  private var insideThinking = false
  private var pending = ""

  fun consume(token: String): List<ThinkingParseChunk> {
    if (token.isEmpty()) return emptyList()

    val chunks = mutableListOf<ThinkingParseChunk>()
    var remaining = pending + token
    pending = ""

    while (remaining.isNotEmpty()) {
      val tag = if (insideThinking) CLOSE_THINK_TAG else OPEN_THINK_TAG
      val fullTagIndex = remaining.indexOf(tag)
      if (fullTagIndex >= 0) {
        val beforeTag = remaining.substring(0, fullTagIndex)
        if (beforeTag.isNotEmpty()) {
          chunks +=
            if (insideThinking) ThinkingParseChunk(thinking = beforeTag)
            else ThinkingParseChunk(text = beforeTag)
        }
        insideThinking = !insideThinking
        remaining = remaining.substring(fullTagIndex + tag.length)
        continue
      }

      val prefixToKeep = longestTagPrefixSuffix(remaining, tag)
      val emitUntil = remaining.length - prefixToKeep.length
      val emitChunk = remaining.substring(0, emitUntil)
      if (emitChunk.isNotEmpty()) {
        chunks +=
          if (insideThinking) ThinkingParseChunk(thinking = emitChunk)
          else ThinkingParseChunk(text = emitChunk)
      }
      pending = prefixToKeep
      remaining = ""
    }

    return chunks
  }

  fun finish(): ThinkingParseChunk? {
    if (pending.isEmpty()) return null

    val chunk =
      if (insideThinking) ThinkingParseChunk(thinking = pending)
      else ThinkingParseChunk(text = pending)
    pending = ""
    return chunk
  }

  private fun longestTagPrefixSuffix(value: String, tag: String): String {
    val maxCandidateLength = minOf(value.length, tag.length - 1)
    for (len in maxCandidateLength downTo 1) {
      if (value.takeLast(len) == tag.take(len)) {
        return value.takeLast(len)
      }
    }
    return ""
  }
}
