/*
 * Copyright 2019-2021 helloscala.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fusion.json.json4s.http

import java.lang.reflect.InvocationTargetException
import akka.http.scaladsl.marshalling.{ Marshaller, ToEntityMarshaller }
import akka.http.scaladsl.model.{ ContentTypeRange, MediaType, MediaTypes }
import akka.http.scaladsl.unmarshalling.{ FromEntityUnmarshaller, Unmarshaller }
import akka.stream.Materializer
import akka.util.ByteString
import fusion.json.json4s.Json4sUtils
import org.json4s.{ Formats, MappingException, Serialization }

import scala.concurrent.ExecutionContext

/**
 * Automatic to and from JSON marshalling/unmarshalling using an in-scope *Json4s* protocol.
 *
 * Pretty printing is enabled if an implicit [[Json4sSupports.ShouldWritePretty.True]] is in scope.
 */
object Json4sSupports {
  sealed abstract class ShouldWritePretty

  final object ShouldWritePretty {
    final object True extends ShouldWritePretty
    final object False extends ShouldWritePretty
  }
}

/**
 * Automatic to and from JSON marshalling/unmarshalling using an in-scope *Json4s* protocol.
 *
 * Pretty printing is enabled if an implicit [[Json4sSupports.ShouldWritePretty.True]] is in scope.
 */
trait Json4sSupport {
  import Json4sSupports._

  def jsonUtils: Json4sUtils

  def unmarshallerContentTypes: Seq[ContentTypeRange] = mediaTypes.map(ContentTypeRange.apply)

  def mediaTypes: Seq[MediaType.WithFixedCharset] = List(MediaTypes.`application/json`)

  private val jsonStringUnmarshaller =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(unmarshallerContentTypes: _*).mapWithCharset {
      case (ByteString.empty, _) => throw Unmarshaller.NoContentException
      case (data, charset)       => data.decodeString(charset.nioCharset.name)
    }

  private val jsonStringMarshaller =
    Marshaller.oneOf(mediaTypes: _*)(Marshaller.stringMarshaller)

  /**
   * HTTP entity => `A`
   *
   * @tparam A type to decode
   * @return unmarshaller for `A`
   */
  implicit def unmarshaller[A: Manifest](
      implicit
      serialization: Serialization = jsonUtils.serialization,
      formats: Formats = jsonUtils.defaultFormats): FromEntityUnmarshaller[A] =
    jsonStringUnmarshaller.map(s => serialization.read(s)).recover(throwCause)

  /**
   * `A` => HTTP entity
   *
   * @tparam A type to encode, must be upper bounded by `AnyRef`
   * @return marshaller for any `A` value
   */
  implicit def marshaller[A <: AnyRef](
      implicit
      serialization: Serialization = jsonUtils.serialization,
      formats: Formats = jsonUtils.defaultFormats,
      shouldWritePretty: ShouldWritePretty = ShouldWritePretty.False): ToEntityMarshaller[A] =
    shouldWritePretty match {
      case ShouldWritePretty.False =>
        jsonStringMarshaller.compose(serialization.write[A])
      case ShouldWritePretty.True =>
        jsonStringMarshaller.compose(serialization.writePretty[A])
    }

  private def throwCause[A](ec: ExecutionContext)(mat: Materializer): PartialFunction[Throwable, A] = {
    case MappingException(_, e: InvocationTargetException) => throw e.getCause
  }
}
