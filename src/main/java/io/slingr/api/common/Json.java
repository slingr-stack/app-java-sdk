package io.slingr.api.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.commons.io.FileUtils.toFile;

/**
 * Class that makes it easy to create JSON objects. For example:
 * <p/>
 * <code>
 * Json.map()
 * .set("prop1", val1)
 * .set("prop2", val2)
 * .set("nested1", Json.map()
 * .set("prop1", val1)
 * .set("prop2", val2))
 * .set("array1", Json.list()
 * .push("element1")
 * .push("element2"))
 * .set("array2", Json.list(users, new Json.ListGenerator() {
 * public Object element(User user) {
 * return Json.map()
 * .set("name", user.getFirstName() + " " + user.getLastName())
 * .set("email", user.getEmail());
 * }
 * })
 * );
 * </code>
 * <p/>
 * Arrays are still a bit cumbersome, but hopefully this will be better with lambdas in Java 8.
 * <p/>
 * User: dgaviola
 * Date: 1/21/13
 */
@SuppressWarnings("unchecked")
public class Json {
    private static final Logger logger = LoggerFactory.getLogger(Json.class);

    private Map<String, Object> map;
    private List<Object> list;

    private final static String NULL_TOKEN = "null";
    private final static ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();

        // Create the module
        SimpleModule mod = new SimpleModule("JSON parser Module");

        // Add the custom serializer to the module
        mod.addSerializer(new CustomSerializer(Json.class));

        OBJECT_MAPPER.registerModule(mod);    // Register the module on the mapper
    }

    public interface ListGenerator<T> {
        Object element(T it);
    }

    public interface MapCreator<T> {
        void create(T obj, Json json);
    }

    private Json() {
    }

    // Constructors

    public static Json map() {
        Json json = new Json();
        json.map = new LinkedHashMap<>();
        return json;
    }

    public static <T> Json map(T obj, MapCreator<T> mapCreator) {
        if (obj == null) {
            return null;
        }
        Json json = map();
        mapCreator.create(obj, json);
        return json;
    }

    public static Json list() {
        Json json = new Json();
        json.list = new ArrayList<>();
        return json;
    }

    public static <T> Json list(Collection<T> items, ListGenerator<T> listGenerator) {
        if (items == null) {
            return null;
        }
        Json json = list();
        for (T item : items) {
            Object res = listGenerator.element(item);
            if (res != null) {
                json.list.add(res);
            }
        }
        return json;
    }

    // Methods to write JSON

    public Json set(String prop, Object value) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        // NOTE: treat enum classes as string. This is to avoid mapping issues.
        if (value instanceof Enum<?>) {
            value = ((Enum) value).name();
        }
        map.put(prop, value);
        return this;
    }

    public Json setIf(boolean condition, String prop, Object value) {
        if (condition) {
            set(prop, value);
        }
        return this;
    }

    public void setByPath(String path, Object obj) {
        setByPath(path, obj, Json.map());
    }

    public void setByPath(String path, Object obj, Json options) {
        Stack<String> explodedPath = PathUtils.getExplodedPath(path);
        setValueByPathRecursive(this, explodedPath, obj, options);
    }


    public Json remove(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (contains(prop)) {
            map.remove(prop);
        }
        return this;
    }

    public void removeByPath(String path) {
        Stack<String> explodedPath = PathUtils.getExplodedPath(path);
        removeValueByPathRecursive(this, explodedPath);
    }

    public Json push(Object value) {
        if (!isList()) {
            throw new IllegalStateException("Operation not supported for a map");
        }
        list.add(value);
        return this;
    }

    public Json pushAll(Json value) {
        if (!isList() || !value.isList()) {
            throw new IllegalStateException("Operation not supported for a map");
        }
        list.addAll(value.list);
        return this;
    }

    // Methods to read JSON

    public Object findPropInRootLevel(String prop) {
        if (isList()) {
            for (Json element : jsons()) {
                if (element.contains(prop)) {
                    if (!element.isEmpty(prop)) {
                        return element.object(prop);
                    }
                    break;
                }
            }
        } else {
            if (contains(prop)) {
                if (!isEmpty(prop)) {
                    return object(prop);
                }
            }
        }
        return null;
    }

    public boolean contains(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return map.containsKey(prop);
    }

    public boolean isEmpty(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return ValidationUtils.isEmptyProperty(map, prop) || ValidationUtils.isEmpty(map.get(prop));
    }

    public boolean isNotEmpty(String prop) {
        return !isEmpty(prop);
    }

    public Object object(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return map.get(prop);
    }

    public List<Object> objects(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (map.get(prop) instanceof Json) {
            return ((Json) map.get(prop)).list;
        }
        return new ArrayList<>();
    }

    public List<Object> objects() {
        if (!isList()) {
            throw new IllegalStateException("Operation not supported for a map");
        }
        return list;
    }

    public Object objectByPath(String path) {
        Stack<String> explodedPath = PathUtils.getExplodedPath(path);
        return getValueRecursive(this, explodedPath);
    }

    public Json json(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        final Object o = map.get(prop);
        if (o != null) {
            return Json.fromObject(o, false);
        }
        return null;
    }

    public List<Json> jsons(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (!(map.get(prop) instanceof Json)) {
            return null;
        }
        List jsons = ((Json) map.get(prop)).list;
        return jsons;
    }


    public List<Json> jsons() {
        return (List) list;
    }

    public String string(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return (String) map.get(prop);
    }

    public List<String> strings(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (!(map.get(prop) instanceof Json)) {
            return null;
        }
        List<String> result = new ArrayList<>();
        if (((Json) map.get(prop)).list != null) {
            for (Object o : ((Json) map.get(prop)).list) {
                if (o != null) {
                    String toString = o.toString();
                    result.add(toString);
                }
            }
        }
        return result;
    }

    public boolean is(String prop) {
        return Boolean.TRUE.equals(bool(prop));
    }

    public Boolean bool(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return (Boolean) map.get(prop);
    }

    public List<Boolean> bools(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (!(map.get(prop) instanceof Json)) {
            return null;
        }
        List bools = ((Json) map.get(prop)).list;
        return bools;
    }

    public Integer integer(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return Helper.getInteger(map.get(prop));
    }

    public List<Integer> integers(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (!(map.get(prop) instanceof Json)) {
            return null;
        }
        List integers = ((Json) map.get(prop)).list;
        return integers;
    }

    public Long long_(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return Helper.getLong(map.get(prop));
    }

    public List<Long> longs(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (!(map.get(prop) instanceof Json)) {
            return null;
        }
        List longs = ((Json) map.get(prop)).list;
        return longs;
    }

    public Double double_(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return Helper.getDouble(map.get(prop));
    }

    public Date date(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return (Date) map.get(prop);
    }

    public List<Date> dates(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (!(map.get(prop) instanceof Json)) {
            return null;
        }
        List dates = ((Json) map.get(prop)).list;
        return dates;
    }

    public boolean isString(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return map.get(prop) == null || map.get(prop) instanceof String;
    }

    public boolean isNumber(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        if (map.get(prop) == null || map.get(prop) instanceof Integer || map.get(prop) instanceof Long) {
            return true;
        }
        return false;
    }

    public boolean isJson(String prop) {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return map.get(prop) == null || map.get(prop) instanceof Json;
    }

    // Utility methods

    public boolean isEmpty() {
        if (isMap()) {
            return map.isEmpty();
        } else {
            return list.isEmpty();
        }
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    public boolean isMap() {
        return map != null;
    }

    public boolean isList() {
        return list != null;
    }

    public static boolean isMap(Object value) {
        if (value == null) {
            return false;
        }
        if (!(value instanceof Json)) {
            return false;
        }
        return ((Json) value).isMap();
    }

    public static boolean isList(Object value) {
        if (value == null) {
            return false;
        }
        if (!(value instanceof Json)) {
            return false;
        }
        return ((Json) value).isList();
    }

    public void merge(Json json) {
        if (isMap() && json.isMap()) {
            for (Map.Entry<String, Object> entry : json.map.entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        } else if (isList() && json.isList()) {
            list.addAll(json.list);
        } else {
            throw new IllegalArgumentException("You cannot merge a list with a map");
        }
    }

    public Set<String> keys() {
        if (isList()) {
            throw new UnsupportedOperationException("You cannot get keys of a list");
        }
        return map.keySet();
    }

    // Methods to convert

    public Map<String, Object> toMap() {
        if (!isMap()) {
            throw new IllegalStateException("Operation not supported for a list");
        }
        return (Map<String, Object>) getMapValue(map);
    }

    public List<Map> toMaps() {
        List<Map> list = new ArrayList<>();
        List l = toList();
        if (l != null) {
            for (Object o : l) {
                list.add(Json.fromObject(o).toMap());
            }
        }
        return list;
    }

    public List<Object> toList() {
        if (!isList()) {
            throw new IllegalStateException("Operation not supported for a map");
        }
        return (List<Object>) getListValue(list);
    }

    public Object toObject() {
        final Object object = getJsonValue(this);
        if (object == null) {
            return new HashMap<>();
        }
        return object;
    }

    private static Object getJsonValue(Json json) {
        if (json == null) {
            return null;
        }
        Object value = null;
        if (json.isMap()) {
            value = json.toMap();
        } else {
            if (json.isList()) {
                value = json.toList();
            }
        }
        return value;
    }

    private static Object getMapValue(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        final Map<String, Object> res = new LinkedHashMap<>();
        map.forEach((k, o) -> {
            if (o instanceof Json) {
                res.put(k, getJsonValue((Json) o));
            } else if (o instanceof Map) {
                res.put(k, getMapValue((Map<String, Object>) o));
            } else if (o instanceof List) {
                res.put(k, getListValue((List<Object>) o));
            } else {
                res.put(k, o);
            }
        });
        return res;
    }

    private static Object getListValue(List<Object> list) {
        if (list == null) {
            return null;
        }
        final List<Object> res = new ArrayList<>();
        list.stream()
                .filter(Objects::nonNull)
                .forEach(o -> {
                    if (o instanceof Json) {
                        res.add(getJsonValue((Json) o));
                    } else if (o instanceof Map) {
                        res.add(getMapValue((Map<String, Object>) o));
                    } else if (o instanceof List) {
                        res.add(getListValue((List<Object>) o));
                    } else {
                        res.add(o);
                    }
                });
        return res;
    }

    public static Json fromMap(Map<String, ?> map) {
        final Json json = Json.map();
        if (map == null) {
            return json;
        }
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = null;
            if (entry.getValue() != null) {
                if (Map.class.isAssignableFrom(entry.getValue().getClass())) {
                    value = fromMap((Map<String, Object>) entry.getValue());
                } else if (Collection.class.isAssignableFrom(entry.getValue().getClass())) {
                    value = fromList((Collection<Object>) entry.getValue());
                } else if (ObjectId.class.isAssignableFrom(entry.getValue().getClass())) {
                    value = entry.getValue().toString();
                } else {
                    value = entry.getValue();
                }
            }
            json.map.put(entry.getKey(), value);
        }
        return json;
    }

    public static Json fromList(Collection<?> list) {
        Json json = Json.list();
        if (list == null) {
            return json;
        }
        for (Object item : list) {
            Object value = null;
            if (item != null) {
                if (Map.class.isAssignableFrom(item.getClass())) {
                    value = fromMap((Map<String, Object>) item);
                } else if (Collection.class.isAssignableFrom(item.getClass())) {
                    value = fromList((Collection<Object>) item);
                } else {
                    value = item;
                }
            }
            json.list.add(value);
        }
        return json;
    }

    public static Json parse(String stringBody) {
        return parse(stringBody, false);
    }

    public static Json parse(String stringBody, boolean allowEscaped) {
        return parse(stringBody, allowEscaped, false);
    }

    public static Json parse(String stringBody, boolean allowEscaped, boolean throwException) {
        // if the string starts with a square brakets we assume it is an array
        if (StringUtils.isNotBlank(stringBody) && stringBody.trim().startsWith("[")) {
            List list = stringToList(stringBody, allowEscaped);
            return fromList(list);
        } else {
            Map<String, Object> map = stringToMap(stringBody, allowEscaped, throwException);
            return fromMap(map);
        }
    }

    public static Json fromFile(String filename) throws IOException {
        File file = toFile(Json.class.getResource(filename));
        String fileContent = FileUtils.readFileToString(file);
        return parse(fileContent);
    }

    public static Json fromObject(Object object) {
        return fromObject(object, true);
    }

    public static Json fromObject(Object object, boolean clone) {
        if (object == null) {
            return Json.map();
        }

        if (object instanceof Json) {
            if (clone) {
                return ((Json) object).cloneJson();
            } else {
                return ((Json) object);
            }
        } else if (object instanceof Map) {
            return Json.fromMap((Map<String, ? extends Object>) object);
        } else if (object instanceof Collection) {
            return Json.fromList((Collection<Object>) object);
        } else {
            return Json.parse(object.toString());
        }
    }

    public static boolean isValidJson(final String json) {
        boolean valid = false;
        try {
            final JsonParser parser = new ObjectMapper().getFactory().createParser(json);
            while (parser.nextToken() != null) {
            }
            valid = true;
        } catch (Exception e) {
            // do nothing
        }
        return valid;
    }

    @Override
    public String toString() {
        return objectToString(getJsonValue(this));
    }

    public static String objectToString(Object object) {
        if (object == null) {
            return NULL_TOKEN;
        }
        if (object instanceof Json) {
            return object.toString();
        }
        String result = null;
        try {
            result = OBJECT_MAPPER.writeValueAsString(object);
        } catch (Exception e) {
            logger.warn("Could not convert object to string", e);
        }
        return result;
    }

    public static Json mapStringToJson(String jsonString) {
        Map<String, Object> map = Json.stringToMap(jsonString);
        if (map == null) {
            throw new IllegalArgumentException(String.format("Object [%s] is not well-formed", jsonString));
        }
        return Json.fromMap(map);
    }

    public static Map<String, Object> stringToMap(String jsonString) {
        return stringToMap(jsonString, false);
    }

    public static Map<String, Object> stringToMap(String jsonString, boolean allowEscaped) {
        return stringToMap(jsonString, allowEscaped, false);
    }

    public static Map<String, Object> stringToMap(String jsonString, boolean allowEscaped, boolean throwException) {
        return stringToMap(jsonString, allowEscaped, throwException, false);
    }

    public static Map<String, Object> stringToMap(String jsonString, boolean allowEscaped, boolean throwException, boolean silentError) {
        if (StringUtils.isBlank(jsonString) || jsonString.equals(NULL_TOKEN)) {
            return new HashMap<>();
        }
        Map<String, Object> result = null;
        try {
            configureToAllowEscapeChars(allowEscaped);
            result = OBJECT_MAPPER.readValue(jsonString, Map.class);
        } catch (Exception e) {
            if (throwException) {
                throw new RuntimeException(String.format("Could not convert string [%s] to map", jsonString), e);
            } else {
                if (!silentError) {
                    logger.warn(String.format("Could not convert string [%s] to map", jsonString), e);
                }
            }
        }
        return result;
    }

    private static void configureToAllowEscapeChars(boolean allowEscaped) {
        if (allowEscaped) {
            OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
            OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        } else {
            OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, false);
            OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, false);
        }
    }

    public static Json listStringToJson(String jsonString) {
        List list = Json.stringToList(jsonString);
        if (list == null) {
            throw new IllegalArgumentException(String.format("Object [%s] is not well-formed", jsonString));
        }
        return Json.fromList(list);
    }

    public static List stringToList(String jsonString) {
        return stringToList(jsonString, false);
    }

    public static List stringToList(String jsonString, boolean allowEscaped) {
        if (StringUtils.isBlank(jsonString) || jsonString.equals(NULL_TOKEN)) {
            return new ArrayList();
        }
        List result = null;
        try {
            configureToAllowEscapeChars(allowEscaped);
            result = OBJECT_MAPPER.readValue(jsonString, List.class);
        } catch (Exception e) {
            logger.warn(String.format("Could not convert string [%s] to list", jsonString), e);
        }
        return result;
    }

    public Json cloneJson() {
        Json json;
        if (isMap()) {
            json = Json.fromMap(map);
        } else {
            json = Json.fromList(list);
        }
        return json;
    }

    public int size() {
        if (isMap()) {
            return map.size();
        } else {
            return list.size();
        }
    }

    public Double decimal(String prop) {
        Object object = object(prop);
        return getDouble(object);
    }

    public List<Double> decimals(String prop) {
        List<Double> list = new ArrayList<>();
        List l = list(prop);
        if (l != null) {
            for (Object o : l) {
                if (o instanceof Number) {
                    list.add(((Number) o).doubleValue());
                }
            }
        }
        return list;
    }

    private static class CustomSerializer extends StdSerializer<Json> {
        public CustomSerializer(Class<Json> t) {
            super(t);
        }

        @Override
        public void serialize(Json json, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
            Object obj = json.toObject();

            if (obj instanceof List) {
                final JsonSerializer<Object> listSerializer = provider.findValueSerializer(List.class, null);
                listSerializer.serialize(obj, jsonGenerator, provider);
            } else if (obj instanceof Map) {
                final JsonSerializer<Object> listSerializer = provider.findValueSerializer(Map.class, null);
                listSerializer.serialize(obj, jsonGenerator, provider);
            } else {
                jsonGenerator.writeStartObject();
                jsonGenerator.writeEndObject();
            }
        }
    }

    public Json set(JsonParameter parameter, Object value) {
        if (parameter == null) {
            throw new IllegalArgumentException("Parameter is null");
        }

        // validating value
        if (value != null) {
            final Class clazz = parameter.getClazz();
            if (!parameter.isList()) {
                if (clazz.equals(String.class)) {
                    value = value.toString();
                } else if (clazz.equals(Boolean.class)) {
                    value = getBoolean(value);
                } else if (clazz.equals(Date.class)) {
                    value = getDate(value);
                } else if (clazz.equals(Double.class) || clazz.equals(Float.class) || clazz.equals(Number.class)) {
                    value = getDouble(value);
                } else if (clazz.equals(Integer.class) || clazz.equals(Short.class)) {
                    value = getInteger(value);
                } else if (clazz.equals(Long.class)) {
                    value = getLong(value);
                } else if (clazz.equals(Json.class)) {
                    value = fromObject(value, false);
                } else if (clazz.equals(List.class)) {
                    value = fromObject(value, false).toList();
                } else if (clazz.equals(Map.class)) {
                    value = fromObject(value, false).toMap();
                }
            } else {
                if (!(value instanceof List)) {
                    throw new IllegalArgumentException("Invalid ");
                }
            }
        }

        return set(parameter.getName(), value);
    }

    public Json setIfNotNull(JsonParameter parameter, Object value) {
        if (value != null) {
            return set(parameter, value);
        }
        return this;
    }

    public Json setIfNotNull(String parameter, Object value) {
        if (value != null) {
            return set(parameter, value);
        }
        return this;
    }

    public Json setIfNotEmpty(JsonParameter parameter, Object value) {
        if (value != null) {
            if (value instanceof Json) {
                if (!((Json) value).isEmpty()) {
                    return set(parameter, value);
                }
            } else if (value instanceof String) {
                if (StringUtils.isNotBlank((String) value)) {
                    return set(parameter, value);
                }
            } else if (value instanceof Collection) {
                if (!((Collection) value).isEmpty()) {
                    return set(parameter, value);
                }
            } else if (value instanceof Map) {
                if (!((Map) value).isEmpty()) {
                    return set(parameter, value);
                }
            } else {
                return setIfNotNull(parameter, value);
            }
        }
        return this;
    }

    public Json setIfNotEmpty(String parameter, Object value) {
        if (value != null) {
            if (value instanceof Json) {
                if (!((Json) value).isEmpty()) {
                    return set(parameter, value);
                }
            } else if (value instanceof String) {
                if (StringUtils.isNotBlank((String) value)) {
                    return set(parameter, value);
                }
            } else if (value instanceof Collection) {
                if (!((Collection) value).isEmpty()) {
                    return set(parameter, value);
                }
            } else if (value instanceof Map) {
                if (!((Map) value).isEmpty()) {
                    return set(parameter, value);
                }
            } else {
                return setIfNotNull(parameter, value);
            }
        }
        return this;
    }

    private Date getDate(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Date) {
            return (Date) o;
        } else {
            Long l = getLong(o);
            if (l != null) {
                return new Date(l);
            } else {
                return null;
            }
        }
    }

    private Integer getInteger(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Integer) {
            return (Integer) o;
        } else if (o instanceof Number) {
            return ((Number) o).intValue();
        } else if (o instanceof String) {
            return Integer.parseInt((String) o);
        } else {
            return Integer.parseInt(o.toString());
        }
    }

    private Long getLong(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Long) {
            return (Long) o;
        } else if (o instanceof Number) {
            return ((Number) o).longValue();
        } else if (o instanceof String) {
            return Long.parseLong((String) o);
        } else {
            return Long.parseLong(o.toString());
        }
    }

    private Double getDouble(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Double) {
            return (Double) o;
        } else if (o instanceof Number) {
            return ((Number) o).doubleValue();
        } else if (o instanceof String) {
            return Double.parseDouble((String) o);
        } else {
            return Double.parseDouble(o.toString());
        }
    }

    private Boolean getBoolean(Object o) {
        if (o == null) {
            return null;
        } else if (o instanceof Boolean) {
            return (Boolean) o;
        } else if (o instanceof Number) {
            return ((Number) o).longValue() != 0;
        } else if (o instanceof String) {
            return Boolean.parseBoolean((String) o);
        } else {
            return Boolean.parseBoolean(o.toString());
        }
    }

    // parameters: gets (string, bool, date, decimal, integer, json, map, list)

    public <T> T get(JsonParameter parameter) {
        if (parameter == null) {
            throw new IllegalArgumentException("Parameter is null");
        }

        final String name = parameter.getName();
        final Class<T> clazz = parameter.getClazz();
        final boolean list = parameter.isList();

        if (clazz.equals(String.class) && !list) {
            return (T) string(name);
        } else if (clazz.equals(String.class)) {
            return (T) strings(name);
        } else if (clazz.equals(Boolean.class) && !list) {
            return (T) bool(name);
        } else if (clazz.equals(Boolean.class)) {
            return (T) bools(name);
        } else if (clazz.equals(Date.class) && !list) {
            return (T) date(name);
        } else if (clazz.equals(Date.class)) {
            return (T) dates(name);
        } else if ((clazz.equals(Double.class) || clazz.equals(Float.class) || clazz.equals(Number.class)) && !list) {
            return (T) decimal(name);
        } else if (clazz.equals(Double.class) || clazz.equals(Float.class) || clazz.equals(Number.class)) {
            return (T) decimals(name);
        } else if ((clazz.equals(Integer.class) || clazz.equals(Short.class)) && !list) {
            return (T) integer(name);
        } else if (clazz.equals(Integer.class) || clazz.equals(Short.class)) {
            return (T) integers(name);
        } else if (clazz.equals(Long.class) && !list) {
            return (T) long_(name);
        } else if (clazz.equals(Long.class)) {
            return (T) longs(name);
        } else if (clazz.equals(Json.class) && !list) {
            return (T) json(name);
        } else if (clazz.equals(Json.class)) {
            return (T) jsons(name);
        } else if (clazz.equals(List.class)) {
            return (T) list(name);
        } else if (clazz.equals(Map.class)) {
            return (T) map(name);
        } else if (!list) {
            return (T) object(name);
        } else {
            return (T) objects(name);
        }
    }

    public List<? extends Object> list(String prop) {
        Object object = object(prop);
        if (object instanceof Json) {
            return ((Json) object).toList();
        } else if (object instanceof List) {
            return (List) object;
        }
        return null;
    }

    public Map map(String prop) {
        Object object = object(prop);
        if (object instanceof Json) {
            return ((Json) object).toMap();
        } else if (object != null) {
            return (Map) object;
        }
        return null;
    }

    public String string(JsonParameter parameter) {
        Object o = get(parameter);
        if (o != null) {
            return o.toString();
        } else {
            return null;
        }
    }

    public List<String> strings(JsonParameter parameter) {
        return get(parameter);
    }

    public boolean is(JsonParameter prop) {
        return Boolean.TRUE.equals(bool(prop));
    }

    public Boolean bool(JsonParameter parameter) {
        return get(parameter);
    }

    public List<Boolean> bools(JsonParameter parameter) {
        return get(parameter);
    }

    public Date date(JsonParameter parameter) {
        return get(parameter);
    }

    public List<Date> dates(JsonParameter parameter) {
        return get(parameter);
    }

    public Double decimal(JsonParameter parameter) {
        return get(parameter);
    }

    public List<Double> decimals(JsonParameter parameter) {
        return get(parameter);
    }

    public Long long_(JsonParameter parameter) {
        return get(parameter);
    }

    public List<Long> longs(JsonParameter parameter) {
        return get(parameter);
    }

    public Integer integer(JsonParameter parameter) {
        return get(parameter);
    }

    public List<Integer> integers(JsonParameter parameter) {
        return get(parameter);
    }

    public Json json(JsonParameter parameter) {
        return get(parameter);
    }

    public List<Json> jsons(JsonParameter parameter) {
        return get(parameter);
    }

    public static class Visitor {

        public void visit(String key, Object value, String path) {
        }

        public Object convertValue(String key, Object value, String path) {
            return value;
        }
    }

    public void traverse(Json.Visitor visitor) {
        traverseRecursive(visitor, "", this);
    }

    private void traverseRecursive(Json.Visitor visitor, String path, Json currentLevel) {
        if (currentLevel.isMap()) {
            Map<String, Object> originalLevel = currentLevel.map;
            Map<String, Object> newLevel = new HashMap<>();
            for (String key : originalLevel.keySet()) {
                Object value = originalLevel.get(key);
                Object newValue;
                if (value instanceof Json || value instanceof Map || value instanceof List) {
                    Json nested = Json.fromObject(value);
                    traverseRecursive(visitor, PathUtils.buildPath(path, key), nested);
                    newValue = visitor.convertValue(key, nested, PathUtils.buildPath(path, key));
                } else {
                    newValue = visitor.convertValue(key, value, PathUtils.buildPath(path, key));
                    ;
                }
                visitor.visit(key, newValue, path);
                newLevel.put(key, newValue);
            }
            currentLevel.map = newLevel;
        } else {
            List<Object> originalList = currentLevel.list;
            List<Object> newLevel = new ArrayList<>();
            ListIterator<Object> it = originalList.listIterator();
            int index = 0;
            while (it.hasNext()) {
                Object item = it.next();
                Object newValue;
                if (item instanceof Json || item instanceof Map || item instanceof List) {
                    Json nested = Json.fromObject(item);
                    traverseRecursive(visitor, PathUtils.buildPath(path, index), nested);
                    newValue = visitor.convertValue(PathUtils.buildPath(PathUtils.lastPathPart(path), index), nested, PathUtils.buildPath(path, index));
                } else {
                    newValue = visitor.convertValue(PathUtils.lastPathPart(path), item, PathUtils.buildPath(path, index));
                }
                newLevel.add(newValue);
                index++;
            }
            currentLevel.list = newLevel;
        }
    }


    // Helpers

    private void setValueByPathRecursive(Json map, Stack<String> path, Object value, Json options) {
        if (map == null || path.isEmpty()) {
            return;
        }
        String key = path.pop();
        if (PathUtils.hasIndex(key)) {
            Json list = (Json) map.object(PathUtils.removeIndex(key));
            if (list == null && path.isEmpty()) {
                list = Json.list();
                map.set(PathUtils.removeIndex(key), list);
            } else if (list == null) {
                list = Json.list();
                map.set(PathUtils.removeIndex(key), list);
            }
            int index = PathUtils.getIndex(key);
            if (path.isEmpty()) {
                Boolean shiftElements = Boolean.TRUE.equals(options.object("shiftElements"));
                if (shiftElements) {
                    list.list.add(index, value);
                } else {
                    list.list.set(index, value);
                }
            } else {
                Json nestedMap = (Json) list.list.get(index);
                if (nestedMap == null) {
                    nestedMap = Json.map();
                    list.list.set(index, nestedMap);
                }
                setValueByPathRecursive(nestedMap, path, value, options);
            }
        } else {
            if (path.isEmpty()) {
                map.set(key, value);
            } else {
                Object nestedValue = map.object(key);
                if (nestedValue == null) {
                    Json nestedMap = Json.map();
                    map.set(key, nestedMap);
                    setValueByPathRecursive(nestedMap, path, value, options);
                } else if (nestedValue instanceof Json) {
                    Json nestedMap = (Json) nestedValue;
                    setValueByPathRecursive(nestedMap, path, value, options);
                }
            }
        }
    }

    private void removeValueByPathRecursive(Json map, Stack<String> path) {
        if (map == null || path.isEmpty()) {
            return;
        }
        String key = path.pop();
        if (PathUtils.hasIndex(key)) {
            Json list = (Json) map.object(PathUtils.removeIndex(key));
            int index = PathUtils.getIndex(key);
            if (path.isEmpty()) {
                list.list.remove(index);
            } else {
                Object nestedMap = list.list.get(index);
                if (nestedMap instanceof Json) {
                    removeValueByPathRecursive((Json) nestedMap, path);
                }
            }
        } else {
            if (path.isEmpty()) {
                map.remove(key);
            } else {
                Object nestedValue = map.object(key);
                if (nestedValue instanceof Json) {
                    Json nestedMap = (Json) nestedValue;
                    removeValueByPathRecursive(nestedMap, path);
                }
            }
        }
    }

    private Object getValueRecursive(Json map, Stack<String> path) {
        if (map == null || path == null || path.isEmpty()) {
            return null;
        }
        Object res;
        String key = path.pop();
        if (PathUtils.hasIndex(key)) {
            Object value = map.object(PathUtils.removeAllIndexes(key));
            if (value instanceof Json && ((Json) value).isList()) {
                int index = PathUtils.getIndex(key);
                if ((((Json) value).size() <= index)) {
                    res = null;
                } else {
                    res = ((Json) value).list.get(index);
                }
            } else {
                return value;
            }
        } else {
            res = map.object(key);
        }
        if (path.isEmpty()) {
            return res;
        } else {
            // it will be a list when the user specified a multi-valued field in the path without an index
            if (res instanceof Json && ((Json) res).isList()) {
                List<Object> list = ((Json) res).list;
                List<Object> resList = new ArrayList<>();
                for (Object item : list) {
                    Stack<String> pathCopy = new Stack<>();
                    pathCopy.addAll(path);
                    Object resItem = getValueRecursive((Json) item, pathCopy);
                    if (resItem instanceof List) {
                        resList.addAll((List) resItem);
                    } else {
                        resList.add(resItem);
                    }
                }
                return resList;
            } else {
                if (res instanceof Json) {
                    return getValueRecursive((Json) res, path);
                } else {
                    return null;
                }
            }
        }
    }
}
