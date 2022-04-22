package com.fidt.yamleditor;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.IllegalClassException;
import org.apache.commons.lang.StringUtils;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
/**
 * yaml读写工具
 * 重写于 2022/4/22，支持通过反转义符插入‘.’， {@link #supportEscape(boolean)}
 * 现有逻辑较为简单，因此支持反转义符时，不支持复合键中某个键以'\'作为结束字符
 * 由于SplitPattern不是线程安全的，如果要用在多线程环境下，并且并发地改动splitPattern的话，请将Pattern改成ThreadLocal的。
 * Yaml类不是线程安全的，具体会因为什么而导致出错，目前尚未考察，因此没有生成static final 的 Yaml类
 */
@Slf4j
public class YamlEditor {
    private static final DumperOptions dumperOptions = new DumperOptions();
    private Pattern splitPattern = DOT_PATTERN;
    /**
     * 匹配 "\."以外的.
     * 通过使用escapePattern, 可以使用带有'.'的字段，例如， "a.b.baidu\.com" 可以表示 键 为 1.a 2.b 3.baidu.com的复合键。
     */
    final static Pattern UNESCAPE_PATTERN = Pattern.compile("(?<!\\\\)\\.");
    /**
     * 匹配 "\."
     */
    final static Pattern ESCAPE_PATTERN = Pattern.compile("\\\\\\.");
    /**
     * 匹配 "."
     * 预先生成splitPattern，防止String.split方法隐式的compile开销。
     */
    final static Pattern DOT_PATTERN = Pattern.compile("[.]");

    static {
        //设置yaml读取方式为块读取
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        dumperOptions.setPrettyFlow(false);
    }

    public YamlEditor() {
    }

    public YamlEditor(boolean supportEscape) {
        supportEscape(supportEscape);
    }

    /**
     * 是否使用"\."来区分分割点和键中的'.'，例如某个键为"cn\.fidt", 那么就应该视为 cn.fidt, 而是不是拆分为cn 和 fidt。
     * @param is 支持通过escape防止'.'被转义为delimiter
     */
    public void supportEscape(boolean is) {
        if (is) {
            splitPattern = UNESCAPE_PATTERN;
        } else {
            splitPattern = DOT_PATTERN;
        }
    }

    /**
     * 将yaml配置文件转化成map
     * @ParamList:
     * @param fileName 默认是resources目录下的yaml文件, 如果yaml文件在resources子目录下，需要加上子目录 比如：conf/config.yaml
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Author Sennri
     * @Date 2020/11/1 22:48
     */
    public static Map<String, Object> getMapFromYaml(String fileName) throws IOException {
        return getMapFromYaml(Paths.get(fileName));
    }

    /**
     * 从Yaml中提取Map对象
     * @Author Sennri
     * @Date 2020/11/2 23:01
     * @ParamList:
     * @param path
     * @return java.util.Map<java.lang.String, java.lang.Object>
     */
    public static Map<String, Object> getMapFromYaml(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            // 这里应该是有问题的？
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlMap = new Yaml(dumperOptions).loadAs(in, LinkedHashMap.class);
            return yamlMap;
        } catch (IOException e) {
            log.error(path + " load failed !");
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * 将Map存入fileName指定的文件当中
     * @Author Sennri
     * @ParamList:
     * @param yamls    map or list, commonly LinkedHashMap
     * @param fileName
     * @return void
     * @Date 2020/11/1 22:49
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, String fileName) throws IOException {
        dumpMapToYaml(yamls, fileName, false);
    }

    /**
     * 将Map存入fileName指定的文件当中，需要判断：是否连目录一起创建
     *
     * @Author
     * @Date 2020/12/7 15:51
     * @ParamList:
     * @param yamls map or list, commonly LinkedHashMap
     * @param fileName  路径+文件名
     * @param isForced  强制创建相关目录
     * @return void
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, String fileName, boolean isForced) throws IOException {
        dumpMapToYaml(yamls, Paths.get(fileName), isForced);
    }

    /**
     * 以URI作为参数确定输出文件路径,将yamlMap写入文件
     *
     * @Author
     * @Date 2020/11/3 11:35
     * @ParamList:
     * @param yamls map形式存储的YAML文件内容
     * @param uri   输出文件uri
     * @return void
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, URI uri) throws IOException {
        dumpMapToYaml(yamls, Paths.get(uri));
    }

    /**
     * 以Path为参数，将yamlMap写入文件
     *
     * @Author
     * @Date 2020/11/3 11:35
     * @ParamList:
     * @param yamls yaml map
     * @param path  输出路径
     * @return void
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, Path path) throws IOException {
        // 若不存在该文件，则创造文件。该逻辑不会创建不存在的文件夹。
        try {
            Files.createFile(path);
        } catch (FileAlreadyExistsException ignored) {
        }
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {
            new Yaml(dumperOptions).dump(yamls, bufferedWriter);
        } catch (IOException e) {
            throw new IOException("Can't write content to file!");
        }
    }

    /**
     * 以Path为参数，将yamlMap写入文件
     *
     * @Author Sennri
     * @Date 2020/12/7 16:12
     * @ParamList:
     * @param yamls map形式存储的YAML文件内容
     * @param path  Path形式的文件路径
     * @param isForced 是否强制添加路径（如果路径中文件夹不存在）
     * @return void
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, Path path, boolean isForced) throws IOException {
        // 若不存在该文件，则创造文件。该逻辑不会创建不存在的文件夹。
        Path parentPath = path.toAbsolutePath().getParent();
        if (Files.exists(parentPath)) {
            dumpMapToYaml(yamls, path);
        } else if (isForced) {
            //未经测试，不能保证功能符合预期。
            Files.createDirectories(path.getParent());
            dumpMapToYaml(yamls, path);
        } else {
            throw new NoSuchFileException(parentPath.toString() + " does not exist.");
        }
    }

    /**
     * 通过分隔符'.'将复合键拆分为 String[] ,如果启用了逃逸符'\'，则清理逃逸结果
     *
     * @Author Sennri
     * @Date 2022/4/22 16:21
     * @ParamList
     * @param compositeKey
     * @return java.lang.String[]
     */
    private String[] splitCompositeKey(String compositeKey) {
        String[] keys = splitPattern.split(compositeKey);
        if (splitPattern == UNESCAPE_PATTERN) {
            removeEscape(keys);
        }
        return keys;
    }

    private static void removeEscape(String[] strings) {
        for (int i = 0, stringLength = strings.length; i < stringLength; i++) {
            String s = strings[i];
            strings[i] = ESCAPE_PATTERN.matcher(s).replaceAll(".");
        }
    }

    /**
     * 通过带有'.'的复合键获取target中的对象
     *
     * @param compositeKey 复合键，使用 "."作为键之间的层级分隔符
     * @param target    待取值的对象
     * @return
     * @throws IllegalArgumentException
     */
    public Object getValue(@NotBlank String compositeKey, @NotNull Object target)
            throws IllegalArgumentException {
        if (target == null) {
            throw new NullPointerException();
        }
        String[] keys = splitCompositeKey(compositeKey);
        return getValueDfs(keys, 0, target);
    }

    /**
     * 递归获取值
     *
     * @Author Sennri
     * @Date 2022/4/22 16:20
     * @ParamList
     * @param keys  键
     * @param depth 深度
     * @param target    取值对象
     * @return java.lang.Object
     */
    private Object getValueDfs(@NotEmpty String[] keys, int depth, @NotNull Object target)
            throws IllegalArgumentException {
        String key = keys[depth];
        depth++;
        Object next;
        if (target instanceof Map) {
            Map<?, ?> t = (Map<?, ?>) target;
            // 不存在键？
            if (t.containsKey(key)) {
                next = t.get(key);
            } else {
                log.error("Key {} does not exist, its index in composite key is {}", key, depth - 1);
                throw new IllegalArgumentException("key error.");
            }
        } else if (target instanceof List) {
            int index = Integer.parseInt(key);
            List<?> t = (List<?>) target;
            // 超出索引范围？
            if (index < t.size()) {
                next = ((List<?>) target).get(index);
            } else {
                log.error("Using a wrong index which is out of bounds: {}", index);
                throw new IllegalArgumentException("Using a wrong index which is out of bounds: " + index);
            }
        } else {
            throw new IllegalArgumentException("Target type is not supported. It should be List or Map.");
        }
        if (depth == keys.length) {
            return next;
        } else {
            if (next == null) {
                log.error("The value of index {} is null, please check whether this composite key is right.", depth - 1);
                throw new IllegalArgumentException();
            }
        }
        return getValueDfs(keys, depth, next);
    }


    // TODO: 2020/10/23 这里应该有问题,并不能解决如果下面是一个数组的情况，但是目前应该还没遇到这种情况
    /**
     * 使用递归的方式设置map中的值，仅适合单一属性 key的格式: "server.port"
     *
     * @Author Sennri
     * @param compositeKey
     * @param value
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Date 2020/10/29 15:42
     **/
    @Deprecated
    public Map<String, Object> generateMap(String compositeKey, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] keys = splitCompositeKey(compositeKey);
        result.put(keys[keys.length - 1], value);
        if (keys.length > 1) {
            return generateMap(compositeKey.substring(0, compositeKey.lastIndexOf(".")), result);
        }
        return result;
    }

    /**
     * 向target内寻找键值key，若key为复合形式，则一直向下取键，获得中间的value值，若这中间发现某个键在递归途中实际上不存在，则返回false
     * 如果只是最下级的键不存在，会直接put该键。因此也可以用这个函数在最底层对Map进行put操作
     *
     * @Author Sennri
     * @Date 2022/4/22 11:17
     * @ParamList
     * @param compositeKey
     * @param value
     * @param target
     * @return void
     */
    public void setValue(String compositeKey, Object value, Object target) throws IllegalClassException {
        if (compositeKey.isEmpty()) {
            throw new NullPointerException();
        }
        String[] keys = splitCompositeKey(compositeKey);
        setValueDfs(keys, 0, value, target);
    }

    /**
     * 递归向下为target设置值。
     * @param keys  拆分后的复合键
     * @param depth 深度
     * @param value 待插入值
     * @param target 被插入对象
     * @throws IllegalClassException
     */
    public void setValueDfs(String[] keys, int depth, Object value, Object target) throws IllegalClassException {
        String key = keys[depth];
        ++depth;
        if (target instanceof Map) {
            if (depth == keys.length) {
                ((Map) target).put(key, value);
            } else {
                Object next = ((Map<?, ?>) target).get(key);
                if (next == null) {
                    throw new IllegalArgumentException("设置路径不正确，请保证设置路径存在。若不要求插入路径已经存在，请使用insert！");
                }
                setValueDfs(keys, depth, value, next);
            }
        } else if (target instanceof List) {
            int index = Integer.parseInt(key);
            if (depth == keys.length) {
                ((List<Object>) target).set(index, value);
            } else {
                Object next = ((List<Object>) target).get(index);
                setValueDfs(keys, depth, value, next);
            }
        } else {
            throw new IllegalClassException("Error: target must be Map or List type!");
        }
    }

    /**
     * 更新yaml文件中的特定键，如果键不存在则不修改（不赋值），如果存在则更换为新值。
     *
     * @Author Sennri
     * @Date 2020/11/1 22:53
     * @ParamList
     * @param key      key是properties的方式： aaa.bbb.ccc (key不存在不修改)
     * @param value    新的属性值 （新属性值和旧属性值一样，不修改）
     * @param yamlName yaml文件的名字
     * @return void
     */
    public void updateYaml(@NonNull String key, @NonNull Object value, @NonNull String yamlName)
            throws IOException, NullPointerException, IllegalClassException {
        Map<String, Object> yamlToMap = Objects.requireNonNull(getMapFromYaml(yamlName));

        // 返回待取键值所在的Map或者List，object不可取null  //return the Map or List to which the key belong.
        Object target = Objects.requireNonNull(getValue(key.substring(0, key.lastIndexOf(".")), yamlToMap));
        // 取旧值 get the old value from target object.
        Object oldValue = getValue(key.substring(key.lastIndexOf(".") + 1), target); // 对上一级map取key值，得到value

        if (value.equals(oldValue)) {//新旧值一样 不修改
            log.debug("New Value equals to old Value " + oldValue + " please checkout the value you want to update.");
        } else {
            setValue(key.substring(key.lastIndexOf(".") + 1), value, target);
            try {
                dumpMapToYaml(yamlToMap, Paths.get(yamlName));
            } catch (IOException e) {
                e.printStackTrace();
                log.error("yaml file update failed !");
                log.error("msg : " + e.getMessage());
                log.error("cause : " + e.getCause());
            }
        }
    }

    /**
     * 采用递归方法向下赋值, 遇到不存在的Key路径将强制存在生成路径对应的对象
     * 如果遇到index值超过了原有层级的列表的范围，会将后续路径添加到列表最末尾
     *
     * @Author Sennri
     * @ParamList:
     * @param compositeKey       键
     * @param value     值
     * @param listOrMap 可能是List也可能是Map的对象
     * @return void
     * @Date 2020/11/2 10:59
     */
    public void insertValueToObject(String compositeKey, Object value, Object listOrMap)
            throws NullPointerException, IllegalArgumentException {
        //因为这个函数 可能 会复用，所以空判断在这里进行即可
        if (compositeKey.isEmpty()) {
            throw new NullPointerException();
        }
        String[] keys = splitCompositeKey(compositeKey);
        if (listOrMap instanceof Map) {
            insertValueToMap(keys, 0, value, (Map<String, Object>) listOrMap);
        } else {
            insertValueToList(keys, 0, value, (List<Object>) listOrMap, Integer.parseInt(keys[0]));
        }
    }

    /**
     * 往list插入特定元素
     * 如果插入index大于等于当前的size，则添加到当前该级列表的最末端。
     * @param keys
     * @param depth
     * @param value
     * @param list
     * @param index
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    private void insertValueToList(String[] keys, int depth, Object value, List<Object> list, int index)
            throws NullPointerException, IllegalArgumentException {
        depth++;
        int len = keys.length;
        if (depth == len) {
            if (list.size() <= index) {
                list.add(value);
            } else {
                list.set(index, value);
            }
        } else {
            String nextKey = keys[depth];
            if (StringUtils.isNumeric(nextKey)) {
                List<Object> next;
                if (list.size() <= index) {
                    log.warn("using index out of list range, will insert an unexisted routine");
                    next = new ArrayList<>();
                    list.add(next);
                } else {
                    next = (List<Object>) list.get(index);
                    if (next == null) {
                        next = new ArrayList<>();
                        list.set(index, next);
                    }
                }
                int nextIndex = Integer.parseInt(nextKey);
                insertValueToList(keys, depth, value, next, nextIndex);
            } else {
                Map<String, Object> next;
                if (list.size() <= index) {
                    log.warn("using index out of list range, will insert an unexisted routine");
                    next = new LinkedHashMap<>();
                    list.add(next);
                } else {
                    next = (Map<String, Object>) list.get(index);
                    if (next == null) {
                        next = new LinkedHashMap<>();
                        list.set(index, next);
                    }
                }
                insertValueToMap(keys, depth, value, next);
            }
        }
    }

    /**
     * 往map插入特定元素
     * 如果中途不存在某个键则会生成这个键。
     * @param keys
     * @param depth
     * @param value
     * @param map
     * @throws NullPointerException
     * @throws IllegalArgumentException
     */
    private void insertValueToMap(String[] keys, int depth, Object value, Map<String, Object> map)
            throws NullPointerException, IllegalArgumentException {
        String currentKey = keys[depth];
        depth++;
        int len = keys.length;
        if (depth == len) {
            map.put(currentKey, value);
        } else {
            String nextKey = keys[depth];
            // 这个判断导致不支持负整数，所以之前的通过-1来进行添加到末尾就不成立了。
            if (StringUtils.isNumeric(nextKey)) {
                int nextIndex = Integer.parseInt(nextKey);
                List<Object> next = (List<Object>) map.get(currentKey);
                if (next == null) {
                    next = new ArrayList<>();
                    map.put(currentKey, next);
                }
                insertValueToList(keys, depth, value, next, nextIndex);
            } else {
                Map<String, Object> next = (Map<String, Object>) map.get(currentKey);
                if (next == null) {
                    next = new LinkedHashMap<>();
                    map.put(currentKey, next);
                }
                insertValueToMap(keys, depth, value, next);
            }
        }
    }


    /**
     * 读取map内容并强制性赋值；向下不断使用getValue，
     * 若当前键不存在时，则判断下一个键是否位整形数字，若为数字创建ArrayList，若为字段创建LinkedHashMap
     *
     * @Author Sennri
     * @ParamList:
     * @param key        完整的键所在路径
     * @param value      打算设置的值
     * @param yamlToMap  转换成LinkedHashMap的yaml文件
     * @param outputPath 文件输出路径（如果有的话）
     * @return void
     * @Date 2020/11/1 22:56
     */
    public void insertYaml(String key,
                           Object value,
                           Map<String, Object> yamlToMap,
                           String outputPath) throws IOException {
        insertValueToObject(key, value, yamlToMap);
        dumpMapToYaml(yamlToMap, outputPath);
    }

    /**
     * 重载形式二
     *
     * @Author Sennri
     * @ParamList:
     * @param key 键
     * @param value 值
     * @param inputPath 输入路径（含文件名）
     * @param outputPath 输出路径（含文件名）
     * @return void
     * @Date 2020/11/1 22:55
     */
    public void insertYaml(String key,
                           Object value,
                           String inputPath,
                           String outputPath)
            throws IOException {
        Map<String, Object> yamlToMap = getMapFromYaml(inputPath);
        insertYaml(key, value, yamlToMap, outputPath);
    }

    /**
     * 向特定yaml文件中插入一组键值对，并更新在原文件上
     *
     * @ParamList:
     * @param key
     * @param value
     * @param inputPath
     * @return void
     * @Author Sennri
     * @Date 2020/11/1 22:54
     */
    public void insertYaml(String key,
                           Object value,
                           String inputPath)
            throws IOException {
        insertYaml(key, value, inputPath, inputPath);
    }

    /**
     * 从List对象或者Map对象中移除特定键值对
     *
     * @Author Sennri
     * @Date 2020/11/3 10:59
     * @ParamList:
     * @param compositeKey   复合键
     * @param listOrMap 待处理的非空list或者map对象
     * @return void
     */
    public void removeListOrMapContent(@NotBlank String compositeKey, @NotNull Object listOrMap)
            throws IllegalClassException, NullPointerException {
        Object target;// 返回key所在对象
        String[] keys = splitCompositeKey(compositeKey);
        int len = keys.length;
        if (len > 1) {
            target = getValueDfs(Arrays.copyOf(keys, len - 1), 0, listOrMap);
            // 如果返回值为null也会导致这里出错
            Objects.requireNonNull(target);
        } else {
            target = listOrMap;
        }
        String lastKey = keys[len - 1];
        if (target instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) target;
            map.remove(lastKey);
        } else if (target instanceof List) {
            @SuppressWarnings("unchecked")
            List<Object> list = ((List<Object>) target);
            list.remove(Integer.parseInt(lastKey));
        } else {
            throw new IllegalClassException("Error: target must be Map-type or List-type!");
        }
    }

    /**
     * 读入文件并移除特定key，随后将该yaml文件内容输出到新的文件当中
     *
     * @Author Sennri
     * @ParamList:
     * @param key   待移除键
     * @param inputYamlName 输入文件名（含路径）
     * @param outputYamlName    输出文件名（含路径）
     * @return void
     * @Date 2020/11/1 23:00
     */
    public void removeYamlContent(String key, String inputYamlName, String outputYamlName)
            throws IOException, IllegalClassException, NullPointerException {
        Map<String, Object> yamlToMap = Objects.requireNonNull(getMapFromYaml(inputYamlName));
        removeListOrMapContent(key, yamlToMap);
        dumpMapToYaml(yamlToMap, outputYamlName);
    }

    /**
     * 重载之一，用于同一文件的读入输出
     *
     * @Author Sennri
     * @Date 2020/11/3 10:30
     * @ParamList:
     * @param key   键
     * @param yamlName  待移除内容的Yaml文件名（包含路径）
     * @return void
     */
    public void removeYamlContent(String key, String yamlName) throws Exception {
        removeYamlContent(key, yamlName, yamlName);
    }
}
