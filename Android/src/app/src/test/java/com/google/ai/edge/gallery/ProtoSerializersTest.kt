package com.google.ai.edge.gallery

import androidx.datastore.core.CorruptionException
import com.google.ai.edge.gallery.proto.BenchmarkResults
import com.google.ai.edge.gallery.proto.CutoutCollection
import com.google.ai.edge.gallery.proto.Settings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProtoSerializersTest {

  @Test
  fun settingsSerializerExposesDefaultInstance() {
    assertSame(Settings.getDefaultInstance(), SettingsSerializer.defaultValue)
  }

  @Test
  fun settingsSerializerRoundTripsProto() = runBlocking {
    val original =
      Settings.newBuilder()
        .setHasSeenBenchmarkComparisonHelp(true)
        .addTextInputHistory("hello")
        .build()

    val output = ByteArrayOutputStream()
    SettingsSerializer.writeTo(original, output)

    val decoded = SettingsSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))
    assertEquals(original, decoded)
  }

  @Test(expected = CorruptionException::class)
  fun settingsSerializerRejectsInvalidBytes() = runBlocking {
    SettingsSerializer.readFrom(ByteArrayInputStream("not-a-proto".toByteArray()))
  }

  @Test
  fun benchmarkResultsSerializerRoundTripsProto() = runBlocking {
    val original = BenchmarkResults.newBuilder().build()

    val output = ByteArrayOutputStream()
    BenchmarkResultsSerializer.writeTo(original, output)

    val decoded = BenchmarkResultsSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))
    assertEquals(original, decoded)
  }

  @Test(expected = CorruptionException::class)
  fun benchmarkResultsSerializerRejectsInvalidBytes() = runBlocking {
    BenchmarkResultsSerializer.readFrom(ByteArrayInputStream(byteArrayOf(1, 2, 3)))
  }

  @Test
  fun cutoutsSerializerRoundTripsProto() = runBlocking {
    val original = CutoutCollection.getDefaultInstance()

    val output = ByteArrayOutputStream()
    CutoutsSerializer.writeTo(original, output)

    val decoded = CutoutsSerializer.readFrom(ByteArrayInputStream(output.toByteArray()))
    assertEquals(original, decoded)
  }

  @Test(expected = CorruptionException::class)
  fun cutoutsSerializerRejectsInvalidBytes() = runBlocking {
    CutoutsSerializer.readFrom(ByteArrayInputStream(byteArrayOf(9, 9, 9)))
  }
}
