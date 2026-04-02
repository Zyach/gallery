package com.google.ai.edge.gallery.service

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

data class InferenceResult(
  val output: String?,
  val error: String?,
  val totalMs: Long,
  val ttfbMs: Long,
)

typealias InferenceRunner = (
  prompt: String,
  onPartial: (partial: String, done: Boolean) -> Unit,
  onError: (message: String) -> Unit,
) -> Unit

object LlmHttpInferenceGateway {

  fun execute(
    prompt: String,
    timeoutSeconds: Long = 30,
    executor: Executor,
    inferenceLock: Any,
    resetConversation: () -> Unit,
    runInference: InferenceRunner,
    cancelInference: () -> Unit,
    elapsedMs: () -> Long,
  ): InferenceResult {
    val sb = StringBuilder()
    val inferenceLatch = CountDownLatch(1)
    val lifecycleLatch = CountDownLatch(1)
    var error: String? = null
    val startMs = elapsedMs()
    var firstTokenMs: Long? = null

    executor.execute {
      synchronized(inferenceLock) {
        try {
          resetConversation()
          runInference(
            prompt,
            { partial, done ->
              if (partial.isNotEmpty()) {
                if (firstTokenMs == null) {
                  firstTokenMs = elapsedMs() - startMs
                }
                sb.append(partial)
              }
              if (done) inferenceLatch.countDown()
            },
            { e -> error = e; inferenceLatch.countDown() },
          )
          val completed = inferenceLatch.await(timeoutSeconds, TimeUnit.SECONDS)
          if (!completed && error == null) {
            error = "timeout"
            cancelInference()
            resetConversation()
          } else if (error != null) {
            cancelInference()
          }
        } catch (t: Throwable) {
          error = t.message
          inferenceLatch.countDown()
        } finally {
          lifecycleLatch.countDown()
        }
      }
    }

    val completed = lifecycleLatch.await(timeoutSeconds + 5, TimeUnit.SECONDS)
    if (!completed && error == null) {
      error = "timeout"
    }
    val totalMs = elapsedMs() - startMs
    return InferenceResult(
      output = if (error != null) null else sb.toString(),
      error = error,
      totalMs = totalMs,
      ttfbMs = firstTokenMs ?: -1,
    )
  }
}
