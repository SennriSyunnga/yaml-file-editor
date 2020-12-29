package com.fidt.yamleditor;

import lombok.NonNull;
import org.apache.commons.lang.IllegalClassException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public class YamlEditor {
    protected static Log log = LogFactory.getLog(YamlEditor.class);//protected static Logger log = LoggerFactory.getLogger(YamlEditor.class);

    private static final DumperOptions dumperOptions = new DumperOptions();

    static {
        //设置yaml读取方式为块读取
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        dumperOptions.setPrettyFlow(false);
    }

    private YamlEditor() {
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
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlMap = new Yaml().loadAs(in, LinkedHashMap.class); // 这里应该是有问题的？
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
     * @Author
     * @Description 判断：是否连目录一起创建
     * @Date 2020/12/7 15:51
     * @ParamList:
     * @param yamls
     * @param fileName
     * @param isForced
     * @return void
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, String fileName, boolean isForced) throws IOException {
        dumpMapToYaml(yamls, Paths.get(fileName), isForced);
    }


    /**
     * @Author
     * @Description 接受URI作为参数写入文件
     * @Date 2020/11/3 11:35
     * @ParamList:
     * @param yamls
     * @param uri
     * @return void
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, URI uri) throws IOException {
        dumpMapToYaml(yamls, Paths.get(uri));
    }

    /**
     * @Author
     * @Description
     * @Date 2020/11/3 11:35
     * @ParamList:
     * @param yamls yaml map
     * @param path  输出路径
     * @return void
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, Path path) throws IOException {
        // 若不存在该文件，则创造文件。该逻辑不会创建不存在的文件夹。
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {//try (FileWriter fileWriter = new FileWriter(fileName)) {
            new Yaml(dumperOptions).dump(yamls, bufferedWriter);
        } catch (IOException e) {
            throw new IOException("Can't write content to file!");
        }
    }

    /**
     * @Author Sennri
     * @Description
     * @Date 2020/12/7 16:12
     * @ParamList:
     * @param yamls
     * @param path
     * @param isForced 是否强制添加路径（如果路径中文件夹不存在）
     * @return void
     */
    public static void dumpMapToYaml(Map<String, Object> yamls, Path path, boolean isForced) throws IOException {
        // 若不存在该文件，则创造文件。该逻辑不会创建不存在的文件夹。
        Path parentPath = path.getParent();
        if (Files.exists(parentPath)) {
            dumpMapToYaml(yamls, path);
        } else if (isForced) {
            Files.createDirectory(path.getParent()); //未经测试，不能保证功能符合预期。
            dumpMapToYaml(yamls, path);
        } else {
            throw new NoSuchFileException(parentPath.toString() + " does not exist.");
        }
    }


    /**
     * 泛型化的从Map或者List对象中获取value的函数
     * @Author Sennri
     * @ParamList:
     * @param key
     * @param target
     * @return java.lang.Object
     * @Date 2020/11/1 22:50
     */
    @SuppressWarnings("unchecked")
    public static Object getValue(String key, Object target)
            throws IllegalArgumentException {
        if (target instanceof List) {
            if (key.contains(".")) {
                String[] keys = key.split("[.]");
                Object object;
                try {
                    object = ((List<Object>) target).get(Integer.parseInt(keys[0]));
                } catch (IndexOutOfBoundsException e) {
                    log.error("Key \"" + keys[0] + "\" is error.");
                    throw new IllegalArgumentException("Using a wrong index which is out of bounds: " + keys[0]);
                }
                if (object instanceof Map || object instanceof List) {
                    return getValue(key.substring(key.indexOf(".") + 1), object);
                } else {
                    throw new IllegalClassException("Target type is not supported. It should be List or Map.");
                }
            } else {
                try {
                    return ((List<Object>) target).get(Integer.parseInt(key));
                } catch (IndexOutOfBoundsException e) { // 若超出index，也返回null，这逻辑和Map保持一致
                    throw new IllegalArgumentException("Using a wrong index which is out of bounds: " + key);
                }
            }
        } else if (target instanceof Map) {
            if (key.contains(".")) {
                String[] keys = key.split("[.]");
                if (!((Map<String, Object>) target).containsKey(keys[0])) {
                    throw new IllegalArgumentException("Using a wrong key which is not in the map: " + keys[0]);
                }
                Object object = ((Map<String, Object>) target).get(keys[0]); //如果结果为null怎么办？
                if (object instanceof Map || object instanceof List) {
                    return getValue(key.substring(key.indexOf(".") + 1), object);
                } else {
                    log.error("Key \"" + keys[0] + "\" is error.");
                    throw new IllegalClassException("Target type is not supported. It should be List or Map, maybe you use a wrong key?");
                }
            } else {
                if (!((Map<String, Object>) target).containsKey(key)) {
                    throw new IllegalArgumentException("Using a wrong key which is not in the map: " + key);
                } else {
                    return ((Map<String, Object>) target).get(key);
                }
            }
        } else {
            throw new IllegalClassException("Target type is not supported. It should be List or Map, maybe you use a wrong key?");
        }
    }

    // TODO: 2020/10/23 这里应该有问题,并不能解决如果下面是一个数组的情况，但是目前应该还没遇到这种情况
    /**
     * 使用递归的方式设置map中的值，仅适合单一属性 key的格式: "server.port"
     * @Author Sennri
     * @param key
     * @param value
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Date 2020/10/29 15:42
     **/
    @Deprecated
    public static Map<String, Object> generateMap(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] keys = key.split("[.]");
        result.put(keys[keys.length - 1], value);
        if (keys.length > 1) {
            return generateMap(key.substring(0, key.lastIndexOf(".")), result);
        }
        return result;
    }


    /**
     * 向target内寻找键值key，若key为复合形式，则一直向下取键，获得中间的value值，若这中间发现某个键在递归途中实际上不存在，则返回false
     * 如果只是最下级的键不存在，会直接put该键。因此也可以用这个函数在最底层进行put操作
     * @Author Sennri
     * @ParamList:
     * @param target 需要进行键值修改的待操作对象
     * @param key    目标键
     * @param value  目标值
     * @Date 2020/11/1 22:45
     */
    @SuppressWarnings("unchecked")
    public static void setValue(String key, Object value, Object target) throws IllegalClassException {
        if (key.isEmpty()) // 为了复用该函数来进行普通的put操作
            throw new NullPointerException();
        if (!key.contains(".")) { //说明到达最底的键
            if (target instanceof Map) {
                ((Map<String, Object>) target).put(key, value); // todo：即便没有这个键值对，也会加上去。有一点风险，感觉不建议这么做。可能会因为错误操作而改变原有数据结构。
            } else if (target instanceof List) {
                int index = Integer.parseInt(key);
                if (0 <= index && index <= ((List<Object>) target).size() - 1) {
                    ((List<Object>) target).set(index, value);
                } else { //若超出既有列表边界，又或者键为负，则为新增。
                    ((List<Object>) target).add(value);
                }
            } else {
                throw new IllegalClassException("Error: target must be Map or List type!");
            }
        } else { //未到达最底层
            String[] keys = key.split("[.]");
            Object object;
            if (target instanceof Map) {
                object = ((Map<String, Object>) target).get(keys[0]);
            } else if (target instanceof List)
                object = ((List<Object>) target).get(Integer.parseInt(keys[0]));
            else {
                throw new IllegalClassException("Error: target must be Map or List type!");
            }
            if (object == null) {
                throw new NullPointerException();
            } else {
                setValue(key.substring(key.indexOf(".") + 1), value, object);
            }
        }
    }


    /**
     * 更新yaml文件中的特定键，如果键不存在则不修改（不赋值），如果存在则更换为新值。
     * @Author Sennri
     * @Date 2020/11/1 22:53
     * @ParamList
     * @param key      key是properties的方式： aaa.bbb.ccc (key不存在不修改)
     * @param value    新的属性值 （新属性值和旧属性值一样，不修改）
     * @param yamlName yaml文件的名字
     * @return void
     */
    public static void updateYaml(@NonNull String key, @NonNull Object value, @NonNull String yamlName)
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
     * @Author Sennri
     * @ParamList:
     * @param key       键
     * @param value     值
     * @param listOrMap 可能是List也可能是Map的对象
     * @return void
     * @Date 2020/11/2 10:59
     */
    public static void insertValueToObject(String key, Object value, Object listOrMap)
            throws NullPointerException, IllegalArgumentException {
        if (key.isEmpty()) //因为这个函数 可能 会复用，所以空判断在这里进行即可
            throw new NullPointerException();
        String[] keys = key.split("[.]");
        int len = keys.length;
        Object temp = listOrMap;
        // TODO: 2020/11/27 这里进行了一个循环位置数量的修改：从 i<len改为 i<len-1
        for (int i = 0; i < len - 1; i++) { //
            if ((getValue(keys[i], temp)) != null) { //len-1的时候应该特殊处理
                temp = getValue(keys[i], temp);
            } else {
                try {
                    new Integer(keys[i + 1]); // 若当前键的下一个键为整形数字，意味着当前本该得到却没有提取到的temp实际上是一个ArrayList
                    ArrayList<Object> list = new ArrayList<>();
                    setValue(keys[i], list, temp); // 单层的set应该没有会报错的情况，这里的set
                    temp = list;
                } catch (NumberFormatException e) {
                    Map<String, Object> map = new LinkedHashMap<>(); // 插入一个newlinkedhashMap
                    setValue(keys[i], map, temp);
                    temp = map;
                }
            }
        }//此时i=len-1，出循环
        setValue(keys[keys.length - 1], value, temp); //写入键值
    }

    /**
     * 读取map内容并强制性赋值；向下不断使用getValue，若当前键不存在时，则判断下一个键是否位整形数字，若为数字创建ArrayList，若为字段创建LinkedHashMap
     * @Author Sennri
     * @ParamList:
     * @param key        完整的键所在路径
     * @param value      打算设置的值
     * @param yamlToMap  转换成LinkedHashMap的yaml文件
     * @param outputPath 文件输出路径（如果有的话）
     * @return void
     * @Date 2020/11/1 22:56
     */
    public static void insertYaml(String key,
                                  Object value,
                                  Map<String, Object> yamlToMap,
                                  String outputPath) throws IOException {
        insertValueToObject(key, value, yamlToMap);
        dumpMapToYaml(yamlToMap, outputPath);
    }

    /**
     * 重载形式二
     * @Author Sennri
     * @ParamList:
     * @param key 键
     * @param value 值
     * @param inputPath 输入路径（含文件名）
     * @param outputPath 输出路径（含文件名）
     * @return void
     * @Date 2020/11/1 22:55
     */
    public static void insertYaml(String key,
                                  Object value,
                                  String inputPath,
                                  String outputPath)
            throws IOException {
        Map<String, Object> yamlToMap = getMapFromYaml(inputPath);
        insertYaml(key, value, yamlToMap, outputPath);
    }

    /**
     * @ParamList:
     * @param key
     * @param value
     * @param inputPath
     * @return void
     * @Author Sennri
     * @Date 2020/11/1 22:54
     */
    public static void insertYaml(String key,
                                  Object value,
                                  String inputPath)
            throws IOException {
        Map<String, Object> yamlToMap = getMapFromYaml(inputPath);
        insertYaml(key, value, yamlToMap, inputPath);
    }

    /**
     * 从List对象或者Map对象中一处特定键值
     * @Author Sennri
     * @Date 2020/11/3 10:59
     * @ParamList:
     * @param key   复合键
     * @param listOrMap 待处理的非空list或者map对象
     * @return void
     */
    public static void removeListOrMapContent(String key, @NonNull Object listOrMap)
            throws IllegalClassException, NullPointerException {
        Object target;// 返回key所在对象
        if (key.contains(".")) {
            target = getValue(key.substring(0, key.lastIndexOf(".")), listOrMap); // 这里如果listOrMap为null会抛出IllegalClass
        } else {
            target = listOrMap;
        }
        Objects.requireNonNull(target);
        if (target instanceof Map)
            ((Map<String, Object>) target).remove(key.substring(key.lastIndexOf(".") + 1));
        else if (target instanceof List)
            ((List<Object>) target).remove(Integer.parseInt(key.substring(key.lastIndexOf(".") + 1)));
        else
            throw new IllegalClassException("Error: target must be Map-type or List-type!");
    }

    /**
     * 适用于生成新文件的情况
     * @Author Sennri
     * @ParamList:
     * @param key
     * @param inputYamlName
     * @param outputYamlName
     * @return void
     * @Date 2020/11/1 23:00
     */
    public static void removeYamlContent(String key, String inputYamlName, String outputYamlName)
            throws IOException, IllegalClassException, NullPointerException {
        Map<String, Object> yamlToMap = Objects.requireNonNull(getMapFromYaml(inputYamlName));
        removeListOrMapContent(key, yamlToMap);
        dumpMapToYaml(yamlToMap, outputYamlName);
    }

    /**
     * @Author Sennri
     * @Description 重载之一，用于同一文件的读入输出
     * @Date 2020/11/3 10:30
     * @ParamList:
     * @param key
     * @param yamlName
     * @return void
     */
    public static void removeYamlContent(String key, String yamlName) throws Exception {
        removeYamlContent(key, yamlName, yamlName);
    }
}

//    public static void main(String[] args) throws Exception {
//        LinkedHashMap map = new LinkedHashMap();
//        Object object=YamlEditor.getValue("s",map);//取空值测试
//        YamlEditor.insertValueToObject("Organizations.0.Name","org1",map); //创建新路径测试
//        log.info(map);
//        YamlEditor.insertValueToObject("Organizations.1.Name","org2",map);
//        log.info(map);
//        YamlEditor.insertValueToObject("Organizations.3.Name","org3",map); //index out of range时会自动加在最末尾
//        log.info(map);
//        YamlEditor.insertValueToObject("Organizations.0.ports.0","172.21.18.41",map);
//        log.info(map);
//        YamlEditor.setValue("Organizations.0.Name","FIDT",map); //改值测试
//        YamlEditor.setValue("Organizations.0.ports.1","172.21.18.42",map); //setValue添加key value对测试
//        log.info(YamlEditor.getValue("Organizations.0.ports.1",map));
//        YamlEditor.removeListOrMapContent("Organizations.1",map);
//        log.info(map);
//        log.info(YamlEditor.dumpMapToYaml(map,"test.yaml"));
//        Map temp = YamlEditor.getMapFromYaml("templates/test.yaml");
//        log.info(temp);
//        return;
//    }


//  弃用的代码：Deprecated

//    /**
//     * @param map
//     * @param key
//     * @param value
//     * @return java.util.Map<java.lang.String, java.lang.Object>
//     * @Param
//     * @Author DELL
//     * @Description 这个没有考虑有list的情况
//     * @Date 2020/10/29 15:40
//     **/
//
//
//    public boolean setValue(Map<String, Object> map, String key, Object value) {
//        String[] keys = key.split("[.]");
//        Map temp = map;
//        for (int i = 0; i < keys.length - 1; i++) {
//            if (temp.containsKey(keys[i])) {
//                temp = (Map) temp.get(keys[i]);
//            } else {
//                return false;
//            }
//        }
//        temp.put(keys[keys.length - 1], value);//即便没有这个值
//        return true; // 设置成功
//    }


//    /**
//     * @param map
//     * @param key
//     * @param value
//     * @return boolean
//     * @Author Sennri
//     * @Description
//     * @Date 2020/10/29 20:40
//     * @ParamList:
//     */
//    public boolean setValue(Map<String, Object> map, String key, Object value) {
//        String[] keys = key.split("[.]");
//        if (keys.length == 1) {
//            map.put(keys[keys.length - 1], value);//即便没有这个值
//            return true; // 设置成功
//        } else {
//            Object object;
//            if ((object = map.get(keys[0])) != null) {
//                if (object instanceof Map)
//                    return setValue((Map<String, Object>) object, key.substring(key.indexOf(".") + 1), value);
//                else if (object instanceof List)
//                    return setValue((List<Object>) object, key.substring(key.indexOf(".") + 1), value);
//                else {
//                    System.out.println("Input key error");
//                    return false;
//                }
//            } else {
//                System.out.println("Can't find this key");
//                return false;
//            }
//        }
//    }
//
//    /**
//     * @param list
//     * @param key
//     * @param value
//     * @return boolean
//     * @Author Sennri
//     * @Description
//     * @Date 2020/10/29 20:40
//     * @ParamList:
//     */
//    public boolean setValue(List<Object> list, String key, Object value) {
//        String[] keys = key.split("[.]");
//        if (keys.length == 1) {
//            list.set(Integer.parseInt(keys[0]), value);//即便没有这个值
//            return true; // 设置成功
//        } else {
//            Object object;
//            if ((object = list.get(Integer.parseInt(keys[0]))) != null) {
//                if (object instanceof Map)
//                    return setValue((Map<String, Object>) object, key.substring(key.indexOf(".") + 1), value);
//                else if (object instanceof List)
//                    return setValue((List<Object>) object, key.substring(key.indexOf(".") + 1), value);
//                else {
//                    System.out.println("Input key error");
//                    return false;
//                }
//            } else {
//                System.out.println("Can't find this key");
//                return false;
//            }
//        }
//    }

//    /**
//     * @param key     key格式：aaa.bbb.ccc (如果对于下面是数组的情况呢？
//     * @param yamlMap
//     * @return java.lang.Object
//     * @Param
//     * @Author Sennri
//     * @Description 通过properties的方式获取yaml中的属性值
//     * @Date 2020/10/29 15:06
//     **/
//    public static Object getValue(String key, Map<String, Object> yamlMap) {
//        if (key.contains(".")) {
//            String[] keys = key.split("[.]");
//            Object object = yamlMap.get(keys[0]);
//            if (object instanceof Map) {
//                return getValue(key.substring(key.indexOf(".") + 1), (Map<String, Object>) object);
//            } else if (object instanceof List) {
//                return getValue(key.substring(key.indexOf(".") + 1), (List<Object>) object);
//            } else {
//                return null;
//            }
//        } else {
//            return yamlMap.get(key);
//        }
//    }
//
//    /**
//     * @param key
//     * @param yamlList
//     * @return java.lang.Object
//     * @Param
//     * @Author DELL
//     * @Description 不支持
//     * @Date 2020/10/29 15:22
//     **/
//
//    public static Object getValue(String key, List<Object> yamlList) {
//        if (key.contains(".")) {
//            String[] keys = key.split("[.]");
//            Object object = yamlList.get(Integer.parseInt(keys[0]));
//            if (object instanceof Map) { //是Map
//                return getValue(key.substring(key.indexOf(".") + 1), (Map<String, Object>) object);
//            } else if (object instanceof List) { //也可能是链表
//                return getValue(key.substring(key.indexOf(".") + 1), (List<Object>) object);
//            } else {
//                return null;
//            }
//        } else {
//            return yamlList.get(Integer.parseInt(key));
//        }
//    }