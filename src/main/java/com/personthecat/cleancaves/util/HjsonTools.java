package com.personthecat.cleancaves.util;

import com.personthecat.cleancaves.model.Direction;
import com.personthecat.cleancaves.model.FloatRange;
import com.personthecat.cleancaves.model.Range;
import com.personthecat.cleancaves.model.ScalableFloat;
import org.hjson.HjsonOptions;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.Mirror;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraftforge.common.BiomeDictionary;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.personthecat.cleancaves.util.CommonMethods.extension;
import static com.personthecat.cleancaves.util.CommonMethods.getBiome;
import static com.personthecat.cleancaves.util.CommonMethods.getBiomeType;
import static com.personthecat.cleancaves.util.CommonMethods.getBiomes;
import static com.personthecat.cleancaves.util.CommonMethods.getBlockState;
import static com.personthecat.cleancaves.util.CommonMethods.runEx;
import static com.personthecat.cleancaves.util.CommonMethods.runExF;

@SuppressWarnings("WeakerAccess")
public final class HjsonTools {

    /** The settings to be used when outputting JsonObjects to the disk. */
    public static final HjsonOptions FORMATTER = new HjsonOptions()
            .setAllowCondense(true)
            .setAllowMultiVal(true)
            .setCommentSpace(0)
            .setSpace(2)
            .setBracesSameLine(true)
            .setOutputComments(true);

    private HjsonTools() { }

    /** Writes the JsonObject to the disk, as JSON or Hjson depending on the file extension. */
    public static Result<IOException> writeJson(JsonObject json, Path file) {
        try (Writer tw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            if (extension(file).equals("json")) {
                json.writeTo(tw, Stringify.FORMATTED);
            } else {
                json.writeTo(tw, FORMATTER);
            }
        } catch (final IOException e) {
            return Result.of(e);
        }
        return Result.ok();
    }

    /** Variant of setOrAdd() used for boolean values. */
    public static JsonObject setOrAdd(JsonObject json, String field, boolean value) {
        return setOrAdd(json, field, JsonValue.valueOf(value));
    }

    /** Modifies or adds a field with the input value. Avoids duplicate fields. */
    public static JsonObject setOrAdd(JsonObject json, String field, JsonValue value) {
        if (json.get(field) != null) {
            return json.set(field, value);
        } else {
            return json.add(field, value);
        }
    }

    /** Updates a single value in a json based on a full, dotted path.  */
    public static void setValueFromPath(JsonObject json, List<PathComponent> path, JsonValue value) {
        if (path.isEmpty()) {
            return;
        }
        final PathComponent lastVal = path.get(path.size() - 1);
        setEither(getLastContainer(json, path), lastVal, value);
    }

    /** Retrieves the last JsonObject or JsonArray represented by the path. */
    public static JsonValue getLastContainer(JsonObject json, List<PathComponent> path) {
        if (path.isEmpty()) {
            return json;
        }
        JsonValue current = json;
        for (int i = 0; i < path.size() - 1; i++) {
            final PathComponent peek = path.get(i + 1);
            current = switch (path.get(i)) {
                case PathComponent.Index(int index) -> getOrTryNew(current.asArray(), index, peek);
                // Key -> key means an object; key -> index means an array.
                case PathComponent.Key(String key) ->
                        peek instanceof PathComponent.Key
                                ? getObjectOrNew(current.asObject(), key)
                                : getArrayOrNew(current.asObject(), key);
            };
        }
        return current;
    }

    /**
     * Visits every object reachable at the given path, treating each path element as
     * <em>either</em> an object <em>or</em> an array.
     */
    public static void forEachAtPath(JsonObject json, Consumer<JsonObject> fn, String... path) {
        forEachContainer(json, path, 0, fn);
    }

    /**
     * Visits every object stored under {@code key}, at any depth in the document. Used by the
     * recursive-override syntax, where {@code *key} applies to every descendant that has it.
     */
    public static void forEachRecursive(JsonObject json, String key, Consumer<JsonObject> fn) {
        for (final JsonObject.Member member : json) {
            final JsonValue value = member.getValue();
            if (member.getName().equals(key)) {
                getRegularObjects(json, key).forEach(fn);
            }
            if (value.isObject()) {
                forEachRecursive(value.asObject(), key, fn);
            } else if (value.isArray()) {
                forEachRecursiveInArray(value.asArray(), key, fn);
            }
        }
    }

    /** Safely retrieves a boolean from the input object. */
    public static Optional<Boolean> getBool(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asBoolean);
    }

    /** Safely retrieves an integer from the input json. */
    public static Optional<Integer> getInt(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asInt);
    }

    public static int getIntOr(JsonObject json, String field, int orElse) {
        return getInt(json, field).orElse(orElse);
    }

    /** Retrieves a range of integers from the input object. */
    public static Optional<Range> getRange(JsonObject json, String field) {
        return getValue(json, field)
                .map(HjsonTools::asOrToArray)
                .map(HjsonTools::toIntArray)
                .map(CommonMethods::sort)
                .map(HjsonTools::toRange);
    }

    public static Optional<FloatRange> getFloatRange(JsonObject json, String field) {
        return getValue(json, field)
                .map(HjsonTools::asOrToArray)
                .map(HjsonTools::toFloatArray)
                .map(CommonMethods::sort)
                .map(HjsonTools::toFloatRange);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<Float> getFloat(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asFloat);
    }

    /** Retrieves a float from the input object. Returns `or` if nothing is found. */
    public static float getFloatOr(JsonObject json, String field, float orElse) {
        return getFloat(json, field).orElse(orElse);
    }

    /** Safely retrieves a string from the input json. */
    public static Optional<String> getString(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asString);
    }

    /** Safely retrieves a JsonArray from the input json. */
    public static Optional<JsonArray> getArray(JsonObject json, String field) {
        return getValue(json, field).map(HjsonTools::asOrToArray);
    }

    /**  Retrieves an array or creates a new one, if absent. */
    public static JsonArray getArrayOrNew(JsonObject json, String field) {
        if (!json.has(field)) {
            json.set(field, new JsonArray());
        }
        return getArray(json, field).orElseThrow(() -> runEx("Unreachable."));
    }

    /** Casts or converts a JsonValue to a JsonArray.*/
    public static JsonArray asOrToArray(JsonValue value) {
        return value.isArray() ? value.asArray() : new JsonArray().add(value);
    }

    /** Safely retrieves a boolean from the input json. */
    public static Optional<JsonObject> getObject(JsonObject json, String field) {
        return getValue(json, field).map(JsonValue::asObject);
    }

    /** Retrieves an object from the input object. Returns an empty object, if nothing is found. */
    public static JsonObject getObjectOrNew(JsonObject json, String field) {
        if (!json.has(field)) {
            json.set(field, new JsonObject());
        }
        return getObject(json, field).orElseThrow(() -> runEx("Unreachable."));
    }

    /** Safely retrieves a JsonValue from the input object. */
    public static Optional<JsonValue> getValue(JsonObject json, String field) {
        return Optional.ofNullable(json.get(field));
    }

    /**
     * Safely retrieves an array of JsonObjects from the input json.
     * To-do: Be more consistent and use Optional, instead.
     */
    public static List<JsonObject> getObjectArray(JsonObject json, String field) {
        List<JsonObject> array = new ArrayList<>();
        getValue(json, field).map(HjsonTools::asOrToArray).ifPresent(a -> flatten(array, a));
        return array;
    }

    /** Variant of {@link #getObjectArray} which does not coerce values into objects. */
    public static List<JsonObject> getRegularObjects(JsonObject json, String field) {
        final List<JsonObject> list = new ArrayList<>();
        final JsonArray array = HjsonTools.getValue(json, field).map(HjsonTools::asOrToArray).orElseGet(JsonArray::new);
        flattenRegularObjects(list, array);
        return list;
    }

    public static Optional<List<Integer>> getIntList(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toIntList);
    }

    /** Converts a JsonArray into an array of ints. */
    public static int[] toIntArray(JsonArray array) {
        final int[] ints = new int[array.size()];
        for (int i = 0; i < array.size(); i++) {
            ints[i] = array.get(i).asInt();
        }
        return ints;
    }

    public static float[] toFloatArray(JsonArray array) {
        final float[] floats = new float[array.size()];
        for (int i = 0; i < array.size(); i++) {
            floats[i] = array.get(i).asFloat();
        }
        return floats;
    }

    /** Safely retrieves a List of Strings from the input json. */
    public static Optional<List<String>> getStringArray(JsonObject json, String field) {
        return getValue(json, field).map(v -> toStringArray(asOrToArray(v)));
    }

    /** Converts a JsonArray into a List of Strings. */
    public static List<String> toStringArray(JsonArray array) {
        List<String> strings = new ArrayList<>();
        for (JsonValue value : array) {
            strings.add(value.asString());
        }
        return strings;
    }

    public static Optional<IBlockState> getState(JsonObject json, String field) {
        return getString(json, field).map(id -> getBlockState(id).orElseThrow(() -> noBlockNamed(id)));
    }

    public static Optional<List<IBlockState>> getStateList(JsonObject json, String field) {
        return getStringArray(json, field).map(HjsonTools::toStateList);
    }

    public static Optional<List<Direction>> getDirectionList(JsonObject json, String field) {
        return getStringArray(json, field).map(HjsonTools::toDirections);
    }

    /** Safely retrieves a BlockPos from the input object. */
    public static Optional<BlockPos> getPosition(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toPosition);
    }

    public static Optional<List<BlockPos>> getPositionList(JsonObject json, String field) {
        return getArray(json, field).map(HjsonTools::toPositionList);
    }

    /** Converts the input JsonArray into a BlockPos object. */
    public static BlockPos toPosition(JsonArray coordinates) {
        // Expect exactly 3 elements.
        if (coordinates.size() != 3) {
            throw runEx("Relative coordinates must be specified in an array of 3 elements, e.g. [0, 0, 0].");
        }
        // Convert the array into a BlockPos object.
        return new BlockPos(coordinates.get(0).asInt(), coordinates.get(1).asInt(), coordinates.get(2).asInt());
    }

    public static Optional<List<Biome>> getBiomeList(JsonObject json, String field) {
        return getObject(json, field).map(HjsonTools::toBiomes);
    }

    /** Converts a JsonArray in to a list of BiomeTypes. */
    public static List<BiomeDictionary.Type> toBiomeTypes(JsonArray array) {
        List<BiomeDictionary.Type> types = new ArrayList<>();
        for (JsonValue value : array) {
            types.add(getBiomeType(value.asString()));
        }
        return types;
    }

    /**
     * Constructs the standard PlacementSettings object used by the mod,
     * apply additional values, when possible.
     */
    public static PlacementSettings getPlacementSettings(JsonObject json) {
        // Construct the basic placement values to be modified.
        PlacementSettings settings = new PlacementSettings().setReplacedBlock(Blocks.STONE);
        // Get the additional values for `placement`.
        getFloat(json, "integrity").ifPresent(settings::setIntegrity);
        getState(json, "replacedBlock").ifPresent(s -> settings.setReplacedBlock(s.getBlock()));
        getEnumValue(json, "mirror", Mirror.class).ifPresent(settings::setMirror);
        getBool(json, "ignoreEntities").ifPresent(settings::setIgnoreEntities);

        // Return the final value.
        return settings;
    }

    /** Retrieves a scalable float from the input json. Returns the default values when no object is found. */
    public static ScalableFloat getScalableFloatOr(JsonObject json, String field, ScalableFloat defaults) {
        return getScalableFloat(json, field, defaults).orElse(defaults);
    }

    public static ScalableFloat toScalableFloat(float startVal, ScalableFloat defaults) {
        return new ScalableFloat(startVal, defaults.startValRandFactor(), defaults.factor(), defaults.randFactor(),
                defaults.exponent());
    }

    public static ScalableFloat toScalableFloat(JsonArray array, ScalableFloat defaults) {
        return new ScalableFloat(array.size() > 0 ? array.get(0).asFloat() : defaults.startVal(),
                array.size() > 1 ? array.get(1).asFloat() : defaults.startValRandFactor(),
                array.size() > 2 ? array.get(2).asFloat() : defaults.factor(),
                array.size() > 3 ? array.get(3).asFloat() : defaults.randFactor(),
                array.size() > 4 ? array.get(4).asFloat() : defaults.exponent());
    }

    public static ScalableFloat toScalableFloat(JsonObject json, ScalableFloat defaults) {
        return new ScalableFloat(getFloatOr(json, "startVal", defaults.startVal()),
                getFloatOr(json, "startValRandFactor", defaults.startValRandFactor()),
                getFloatOr(json, "factor", defaults.factor()), getFloatOr(json, "randFactor", defaults.randFactor()),
                getFloatOr(json, "exponent", defaults.exponent()));
    }

    public static <T extends Enum<T>> Optional<T> getEnumValue(JsonObject json, String field, Class<T> clazz) {
        return getString(json, field).map(s -> toEnumValue(s, clazz));
    }

    /** Informs the user that they have entered an invalid biome name. */
    public static RuntimeException noBiomeNamed(String name) {
        return runExF("There is no biome named \"{}.\"", name);
    }

    /** Informs the user that they have entered an invalid biome ID. */
    public static RuntimeException noBiomeID(int id) {
        return runExF("There is no biome with id \"{}.\"", id);
    }

    /** Informs the user that they have entered an invalid block name. */
    public static RuntimeException noBlockNamed(String name) {
        return runExF("There is no block named \"{}.\"", name);
    }

    /** Attempts to retrieve an object or an array. Creates a new one, if absent. */
    private static JsonValue getOrTryNew(JsonArray array, int index, PathComponent type) {
        if (index == array.size()) { // The value must be added.
            switch (type) {
                case PathComponent.Key ignored -> array.add(new JsonObject());
                case PathComponent.Index ignored -> array.add(new JsonArray());
            }
        } // if index >= newSize -> index out of bounds
        return array.get(index);
    }

    /** Attempts to set a value in a container which may either be an object or an array. */
    private static void setEither(JsonValue container, PathComponent either, JsonValue value) {
        switch (either) {
            case PathComponent.Key(String key) -> container.asObject().set(key, value);
            case PathComponent.Index(int index) -> container.asArray().set(index, value);
        }
    }

    private static void forEachContainer(JsonObject container, String[] path, int index, Consumer<JsonObject> fn) {
        if (index == path.length) {
            fn.accept(container);
            return;
        }
        for (final JsonObject o : getRegularObjects(container, path[index])) {
            forEachContainer(o, path, index + 1, fn);
        }
    }

    private static void forEachRecursiveInArray(JsonArray array, String key, Consumer<JsonObject> fn) {
        for (final JsonValue value : array) {
            if (value.isObject()) {
                forEachRecursive(value.asObject(), key, fn);
            } else if (value.isArray()) {
                forEachRecursiveInArray(value.asArray(), key, fn);
            }
        }
    }

    private static Range toRange(int[] range) {
        if (range.length == 0) {
            return Range.of(0);
        }
        return range.length == 1 ? Range.of(range[0]) : Range.of(range[0], range[range.length - 1]);
    }

    private static FloatRange toFloatRange(float[] range) {
        if (range.length == 0) {
            return new FloatRange(0F, 0F);
        }
        return range.length == 1
                ? new FloatRange(range[0], range[0])
                : new FloatRange(range[0], range[range.length - 1]);
    }

    /** Recursively flattens object arrays into a single dimension. */
    private static void flatten(List<JsonObject> array, JsonArray source) {
        for (JsonValue value : source) {
            if (value.isArray()) {
                flatten(array, value.asArray());
            } else {
                // This is assumed to be an object. If it isn't,
                // The user should be informed (i.e. crash).
                array.add(value.asObject());
            }
        }
    }

    /** Variant of {@link #flatten} which does not coerce values into objects. */
    private static void flattenRegularObjects(List<JsonObject> array, JsonArray source) {
        for (JsonValue value : source) {
            if (value.isArray()) {
                flattenRegularObjects(array, value.asArray());
            } else if (value.isObject()) {
                array.add(value.asObject());
            }
        }
    }

    private static List<Integer> toIntList(JsonArray array) {
        final List<Integer> ints = new ArrayList<>();
        for (JsonValue value : array) {
            if (!value.isNumber()) {
                throw runExF("Expected an numeric value: {}", value);
            }
            ints.add(value.asInt());
        }
        return ints;
    }

    private static List<IBlockState> toStateList(List<String> ids) {
        return ids.stream()
                .map(id -> getBlockState(id).orElseThrow(() -> noBlockNamed(id)))
                .collect(Collectors.toList());
    }

    private static List<Direction> toDirections(List<String> directions) {
        return directions.stream().map(s -> toEnumValue(s, Direction.class)).collect(Collectors.toList());
    }

    private static List<BlockPos> toPositionList(JsonArray positions) {
        final List<BlockPos> list = new ArrayList<>();
        for (JsonValue position : positions) {
            if (position.isNumber()) {
                return Collections.singletonList(toPosition(positions));
            } else if (!position.isArray()) {
                throw runEx("Expected a list of positions, e.g. [[0, 0, 0], [1, 1, 1]].");
            }
            list.add(toPosition(position.asArray()));
        }
        return list;
    }

    private static List<Biome> toBiomes(JsonObject json) {
        final List<Biome> biomes = new ArrayList<>();
        // Get biomes by registry name.
        getArray(json, "names").map(HjsonTools::toStringArray).ifPresent(a -> {
            for (String s : a) {
                biomes.add(getBiome(s).orElseThrow(() -> noBiomeNamed(s)));
            }
        });
        // Get biomes by ID.
        getArray(json, "IDs").map(HjsonTools::toIntArray).ifPresent(a -> {
            for (int i : a) {
                biomes.add(getBiome(i).orElseThrow(() -> noBiomeID(i)));
            }
        });
        // Get biomes by type.
        getArray(json, "types").map(HjsonTools::toBiomeTypes).ifPresent(a -> {
            for (BiomeDictionary.Type t : a) {
                Collections.addAll(biomes, getBiomes(t));
            }
        });
        return biomes;
    }

    /**
     * Safely retrieves a ScalableFloat from the input json using default
     * values when necessary.
     */
    private static Optional<ScalableFloat> getScalableFloat(JsonObject json, String field, ScalableFloat defaults) {
        // Attempt to retrieve a corresponding JsonValue from `json`. If
        // it exists, test to see whether it is an object. If it is an
        // object, it should be converted into a ScalableFloat according
        // to the presence of all relevant fields, substituting from
        // `defaults`, when necessary. Else, attempt to parse it as an
        // array. In this case, values are read according to their order
        // and then passed directly into the constructor in-code. Absent
        // values are likewise substituted from `defaults`.
        // Asserting that the value must be an array means that non-array
        // values can also be used at this position, according to the
        // spec of our custom hjson parser. This is especially convenient
        // for users who only want to change the starting value of the
        // resultant float.
        return getValue(json, field).map(v -> {
            if (v.isNumber()) {
                return toScalableFloat(v.asFloat(), defaults);
            } else if (v.isObject()) {
                return toScalableFloat(v.asObject(), defaults);
            } else if (!v.isArray()) {
                throw runEx("Scalable float values must be a number, array, or object.");
            }
            return toScalableFloat(v.asArray(), defaults);
        });
    }

    private static <T extends Enum<T>> T toEnumValue(String s, Class<T> clazz) {
        final T[] constants = clazz.getEnumConstants();
        return Arrays.stream(constants).filter(v -> v.toString().equalsIgnoreCase(s)).findFirst().orElseThrow(() -> {
            final String name = clazz.getSimpleName();
            final String values = Arrays.toString(constants);
            return runExF("{} \"{}\" does not exist. Valid options are: {}", name, s, values);
        });
    }

}
