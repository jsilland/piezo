/**
 * Copyright 2013 Julien Silland
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

package io.soliton.protobuf.json;

import com.google.common.base.CaseFormat;
import com.google.common.io.BaseEncoding;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;

import java.util.List;
import java.util.Map;

/**
 * Static utility methods pertaining to instances of {@link Message} objects.
 *
 * @author Julien Silland (julien@soliton.io)
 */
public class Messages {

  /**
   * Converts a proto-message into an equivalent JSON representation.
   *
   * @param output
   * @return
   */
  public static JsonObject toJson(Message output) {
    JsonObject object = new JsonObject();
    for (Map.Entry<Descriptors.FieldDescriptor, Object> field : output.getAllFields().entrySet()) {
      String jsonName = CaseFormat.LOWER_UNDERSCORE.to(
          CaseFormat.LOWER_CAMEL, field.getKey().getName());
      if (field.getKey().isRepeated()) {
        JsonArray array = new JsonArray();
        List<?> items = (List<?>) field.getValue();
        for (Object item : items) {
          array.add(serializeField(field.getKey(), item));
        }
        object.add(jsonName, array);
      } else {
        object.add(jsonName, serializeField(field.getKey(), field.getValue()));
      }
    }
    return object;
  }

  private static JsonElement serializeField(Descriptors.FieldDescriptor field, Object value) {
    switch (field.getType()) {
      case DOUBLE:
        return new JsonPrimitive((Double) value);
      case FLOAT:
        return new JsonPrimitive((Float) value);
      case INT64:
      case UINT64:
      case FIXED64:
      case SINT64:
      case SFIXED64:
        return new JsonPrimitive((Long) value);
      case INT32:
      case UINT32:
      case FIXED32:
      case SINT32:
      case SFIXED32:
        return new JsonPrimitive((Integer) value);
      case BOOL:
        return new JsonPrimitive((Boolean) value);
      case STRING:
        return new JsonPrimitive((String) value);
      case GROUP:
      case MESSAGE:
        return toJson((Message) value);
      case BYTES:
        return new JsonPrimitive(BaseEncoding.base64().encode(((ByteString) value).toByteArray()));
      case ENUM:
        String protoEnumName = ((Descriptors.EnumValueDescriptor) value).getName();
        return new JsonPrimitive(
            CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, protoEnumName));
    }
    return null;
  }

  public static Message fromJson(Message.Builder builder, JsonObject input) {
    Descriptors.Descriptor descriptor = builder.getDescriptorForType();
    for (Map.Entry<String, JsonElement> entry : input.entrySet()) {
      String protoName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entry.getKey());
      Descriptors.FieldDescriptor field = descriptor.findFieldByName(protoName);
      if (field == null) {
        // fail;
      }
      if (field.isRepeated()) {
        if (!entry.getValue().isJsonArray()) {
          // fail
        }
        JsonArray array = entry.getValue().getAsJsonArray();
        for (JsonElement item : array) {
          builder.addRepeatedField(field, parseField(field, item, builder));
        }
      } else {
        builder.setField(field, parseField(field, entry.getValue(), builder));
      }
    }
    return builder.build();
  }

  private static Object parseField(Descriptors.FieldDescriptor field, JsonElement value,
      Message.Builder enclosingBuilder) {
    switch (field.getType()) {
      case DOUBLE:
        if (!value.isJsonPrimitive()) {
          // fail;
        }
        return value.getAsDouble();
      case FLOAT:
        if (!value.isJsonPrimitive()) {
          // fail;
        }
        return value.getAsFloat();
      case INT64:
      case UINT64:
      case FIXED64:
      case SINT64:
      case SFIXED64:
        if (!value.isJsonPrimitive()) {
          // fail
        }
        return value.getAsLong();
      case INT32:
      case UINT32:
      case FIXED32:
      case SINT32:
      case SFIXED32:
        if (!value.isJsonPrimitive()) {
          // fail
        }
        return value.getAsInt();
      case BOOL:
        if (!value.isJsonPrimitive()) {
          // fail
        }
        return value.getAsBoolean();
      case STRING:
        if (!value.isJsonPrimitive()) {
          // fail
        }
        return value.getAsString();
      case GROUP:
      case MESSAGE:
        if (!value.isJsonObject()) {
          // fail
        }
        return fromJson(enclosingBuilder.getFieldBuilder(field), value.getAsJsonObject());
      case BYTES:
        if (!value.isJsonPrimitive()) {
          // fail
        }
        return ByteString.copyFrom(BaseEncoding.base64().decode(value.getAsString()));
      case ENUM:
        if (!value.isJsonPrimitive()) {
          // fail
        }
        String protoEnumValue = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE,
            value.getAsString());
        return field.getEnumType().findValueByName(protoEnumValue);
    }
    return null;
  }
}
