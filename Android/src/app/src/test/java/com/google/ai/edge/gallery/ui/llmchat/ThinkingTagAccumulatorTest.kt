package com.google.ai.edge.gallery.ui.llmchat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThinkingTagAccumulatorTest {

  @Test
  fun emitsPlainTextWithoutTags() {
    val parser = ThinkingTagAccumulator()

    val chunks = parser.consume("plain answer")

    assertEquals(listOf(ThinkingParseChunk(text = "plain answer")), chunks)
    assertNull(parser.finish())
  }

  @Test
  fun emitsThinkingAndTextWhenTagsAppearInSingleChunk() {
    val parser = ThinkingTagAccumulator()

    val chunks = parser.consume("before<think>reasoning</think>after")

    assertEquals(
      listOf(
        ThinkingParseChunk(text = "before"),
        ThinkingParseChunk(thinking = "reasoning"),
        ThinkingParseChunk(text = "after"),
      ),
      chunks,
    )
  }

  @Test
  fun supportsOpenTagSplitAcrossChunks() {
    val parser = ThinkingTagAccumulator()

    assertEquals(listOf(ThinkingParseChunk(text = "before")), parser.consume("before<th"))
    assertEquals(listOf(ThinkingParseChunk(thinking = "reasoning")), parser.consume("ink>reasoning"))
    assertEquals(listOf(ThinkingParseChunk(text = "after")), parser.consume("</think>after"))
  }

  @Test
  fun supportsCloseTagSplitAcrossChunks() {
    val parser = ThinkingTagAccumulator()

    assertEquals(emptyList<ThinkingParseChunk>(), parser.consume("<think>"))
    assertEquals(listOf(ThinkingParseChunk(thinking = "reason")), parser.consume("reason</th"))
    assertEquals(listOf(ThinkingParseChunk(text = "answer")), parser.consume("ink>answer"))
  }

  @Test
  fun flushesPendingTextOnFinish() {
    val parser = ThinkingTagAccumulator()

    assertEquals(listOf(ThinkingParseChunk(text = "ab")), parser.consume("ab<th"))
    assertEquals(ThinkingParseChunk(text = "<th"), parser.finish())
  }

  @Test
  fun flushesPendingThinkingOnFinish() {
    val parser = ThinkingTagAccumulator()

    assertEquals(listOf(ThinkingParseChunk(thinking = "reason")), parser.consume("<think>reason</th"))
    assertEquals(ThinkingParseChunk(thinking = "</th"), parser.finish())
  }
}
