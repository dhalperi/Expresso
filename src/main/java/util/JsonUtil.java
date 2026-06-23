package util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import org.batfish.common.util.BatfishObjectMapper;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;

public class JsonUtil {
  public static final ObjectMapper mapper =
      BatfishObjectMapper.verboseMapper().enable(SerializationFeature.INDENT_OUTPUT);

  /* get as string helpers */
  public static String getAsStringOrDefault(
      JsonObject object, String key, @Nullable String defaultValue) {
    return (!object.has(key) || object.get(key).isJsonNull())
        ? defaultValue
        : object.get(key).getAsString();
  }

  public static String getAsStringDefaultNull(JsonObject object, String key) {
    return getAsStringOrDefault(object, key, null);
  }

  public static String getAsStringDefaultEmpty(JsonObject object, String key) {
    return getAsStringOrDefault(object, key, "");
  }

  /* get as boolean helpers */
  public static boolean getAsBooleanOrDefault(JsonObject object, String key, boolean defaultValue) {
    return (!object.has(key) || object.get(key).isJsonNull())
        ? defaultValue
        : object.get(key).getAsBoolean();
  }

  public static boolean getAsBooleanDefaultTrue(JsonObject object, String key) {
    return getAsBooleanOrDefault(object, key, true);
  }

  public static boolean getAsBooleanDefaultFalse(JsonObject object, String key) {
    return getAsBooleanOrDefault(object, key, false);
  }

  /* get as number helpers */
  public static int getAsIntOrDefault(JsonObject object, String key, int defaultValue) {
    return (!object.has(key) || object.get(key).isJsonNull())
        ? defaultValue
        : object.get(key).getAsInt();
  }

  public static double getAsDoubleOrDefault(JsonObject object, String key, double defaultValue) {
    return (!object.has(key) || object.get(key).isJsonNull())
        ? defaultValue
        : object.get(key).getAsDouble();
  }

  public static <T> T getAsObjectDefaultNull(
      JsonObject object, String key, Function<JsonElement, T> func) {
    return (!object.has(key) || object.get(key).isJsonNull()) ? null : func.apply(object.get(key));
  }

  public static void writeString(JsonWriter jw, String name, String value) {
    try {
      jw.name(name);
      jw.value(value);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void writeNumber(JsonWriter jw, String name, Number value) {
    try {
      jw.name(name);
      jw.value(value);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void writeEmptyArray(JsonWriter jw, String name) {
    try {
      jw.name(name);
      jw.beginArray();
      jw.endArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void writeNumberArray(JsonWriter jw, String name, Number... values) {
    try {
      jw.name(name);
      jw.beginArray();
      for (Number val : values) {
        jw.value(val);
      }
      jw.endArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static <T> void writeArray(JsonWriter jw, String name, Collection<T> values) {
    try {
      jw.name(name);
      jw.beginArray();
      for (T val : values) {
        jw.value(val.toString());
      }
      jw.endArray();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
