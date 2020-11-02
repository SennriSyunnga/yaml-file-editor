package com.fidt.yamleditor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.apache.commons.lang.StringUtils;


import org.springframework.lang.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


//@Slf4j
//@Component

public class YamlEditor {
    //protected static Logger log = LoggerFactory.getLogger(YamlEditor.class);
    protected static Log log = LogFactory.getLog(YamlEditor.class);

    private static final DumperOptions dumperOptions = new DumperOptions();

    static {
        //设置yaml读取方式为块读取
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        dumperOptions.setPrettyFlow(false);
    }

    /**
     * @param fileName 默认是resources目录下的yaml文件, 如果yaml文件在resources子目录下，需要加上子目录 比如：conf/config.yaml
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Author Sennri
     * @Description 将yaml配置文件转化成map
     * @Date 2020/11/1 22:48
     * @ParamList:
     */

    public static Map<String, Object> getYamlToMap(String fileName) {
        LinkedHashMap<String, Object> yamls = new LinkedHashMap<>();
        // Yaml yaml = new Yaml();
        // @Cleanup InputStream in = YamlEditor.class.getClassLoader().getResourceAsStream(fileName); // 这里舍弃了@写法
        try (InputStream in = YamlEditor.class.getClassLoader().getResourceAsStream(fileName)) {
            yamls = new Yaml().loadAs(in, LinkedHashMap.class); // 这里应该是有问题的？
        } catch (Exception e) {
            log.error(fileName + " load failed !!!");
        }
        return yamls;
    }

    /**
     * @param yamls    map or list, commonly LinkedHashMap
     * @param fileName
     * @return boolean
     * @Author Sennri
     * @Description 通用部分：将map存入fileName当中
     * @Date 2020/11/1 22:49
     * @ParamList:
     */
    public static boolean dumpMapToYaml(Map<String, Object> yamls, String fileName) throws IOException {
        //Yaml yaml = new Yaml(dumperOptions);
        // 若不存在改文件，则创造文件。该逻辑不会创建不存在的文件夹。
        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(path)) {//try (FileWriter fileWriter = new FileWriter(fileName)) {
            new Yaml(dumperOptions).dump(yamls, bufferedWriter);
            return true;
        } catch (IOException e) {
            throw new IOException("Cant write content to file!");
        }
    }


    /**
     * @param key
     * @param target
     * @return java.lang.Object
     * @Author Sennri
     * @Description 泛型化的取value函数，也可以提取出map，不一定要取key。应该不能再进一步泛型化了吧？
     * @Date 2020/11/1 22:50
     * @ParamList:
     */
    public static Object getValue(String key, Object target) throws Exception {
        if (target instanceof List) {
            if (key.contains(".")) {
                String[] keys = key.split("[.]");
                Object object = ((List) target).get(Integer.parseInt(keys[0]));
                if (object instanceof Map || object instanceof List) { //是Map
                    return getValue(key.substring(key.indexOf(".") + 1), object);
                } else {
                    return null;
                }
            } else {
                try {
                    return ((List) target).get(Integer.parseInt(key));
                } catch (IndexOutOfBoundsException e) { // 若超出index，也返回null，这逻辑和Map保持一致
                    return null;
                }
            }
        } else if (target instanceof Map) {
            if (key.contains(".")) {
                String[] keys = key.split("[.]");
                Object object = ((Map) target).get(keys[0]);
                if (object instanceof Map || object instanceof List) {
                    return getValue(key.substring(key.indexOf(".") + 1), object);
                } else {
                    log.error("Cant get the target key.");
                    return null;
                }
            } else {
                return ((Map) target).get(key);
            }
        } else {
            throw new Exception("Collection type error. It should be List or Map.");
        }
    }

    // TODO: 2020/10/23 这里应该有问题,并不能解决如果下面是一个数组的情况，但是目前应该还没遇到这种情况

    /**
     * @param key
     * @param value
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Author Sennri
     * @Description 使用递归的方式设置map中的值，仅适合单一属性 key的格式: "server.port"
     * @Date 2020/10/29 15:42
     * @ParamList:
     **/
    @Deprecated
    public Map<String, Object> generateMap(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] keys = key.split("[.]");
        result.put(keys[keys.length - 1], value);
        if (keys.length > 1) {
            return generateMap(key.substring(0, key.lastIndexOf(".")), result);
        }
        return result;
    }


    /**
     * @param target 需要进行键值修改的对象
     * @param key    目标键
     * @param value  目标值
     * @return boolean
     * @Author Sennri
     * @Description 向target内寻找键值key，若key为复合形式，则一直向下取键，获得value值，若中途的路径不存在则返回false
     * 如果只是最下级的键不存在，会put该键。这个函数可以在最底层进行put
     * @Date 2020/11/1 22:45
     * @ParamList:
     */
    public static boolean setValue(String key, Object value, Object target) throws Exception {
        if (key.isEmpty()) //为了复用
            return false;
        if (!key.contains(".")) { //说明到达最底的键
            if (target instanceof Map) {
                ((Map) target).put(key, value); //即便没有这个值，也会加上去——有一点风险，感觉不建议这么做。会因为错误操作而改变原有结构。
            } else if (target instanceof List) {
                if (Integer.parseInt(key) < ((List) target).size() - 1) {
                    ((List) target).set(Integer.parseInt(key), value);
                } else { //新增
                    ((List) target).add(value);
                }
            } else {
                throw new Exception("Error: target must be Map-type or List-type!");
            }
            return true; // 设置成功
        } else {
            String[] keys = key.split("[.]");
            Object object;
            if (target instanceof Map) {
                object = ((Map) target).get(keys[0]);
            } else if (target instanceof List)
                object = ((List) target).get(Integer.parseInt(keys[0]));
            else {
                throw new Exception("Error: target must be Map-type or List-type!");
            }
            if (object == null) {
                return false; //并无这个键，修改失败
            } else {
                return setValue(key.substring(key.indexOf(".") + 1), value, object);
            }
        }
    }

    /**
     * @param key      key是properties的方式： aaa.bbb.ccc (key不存在不修改)
     * @param value    新的属性值 （新属性值和旧属性值一样，不修改）
     * @param yamlName yaml文件的名字
     * @return boolean
     * @Author Sennri
     * @Description 更新yaml文件中的特定键，如果键不存在则不修改（不赋值），如果存在则更换为新值。
     * @Date 2020/11/1 22:53
     * @ParamList:
     */
    public boolean updateYaml(String key, @Nullable Object value, String yamlName) throws Exception {
        // getYamlToMap 返回的是Object更合适吧？
        Map<String, Object> yamlToMap = getYamlToMap(yamlName);
        // if the yaml file is empty, then return false.
        if (null == yamlToMap) {
            return false;
        }
        // 返回待取键值所在的Map或者List。
        // return the Map or List to which the key belong.
        Object target = getValue(key.substring(0, key.lastIndexOf(".")), yamlToMap);
        // get the old value from target object.
        Object oldValue = getValue(key.substring(key.lastIndexOf(".") + 1), target); // 对上一级map取key值，得到value

        // 未找到key 返回false
        // key not found, return false.
        if (oldValue == null) {
            log.error("Key " + key + " is not found.");
            return false;
        }

        // TODO: 2020/10/29 显然这个判断不了oldVal是可变对象的情况，例如，一个map，只能判断一下非可变类型，例如，string
        //新旧值一样 不修改
        if (value.equals(oldValue)) {
            log.debug("New Value equals to old Value " + oldValue + " please checkout the value you want to update.");
            return false;
        }

        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(yamlName)).getPath();//String path = this.getClass().getClassLoader().getResource(yamlName).getPath();
        if (setValue(key.substring(key.lastIndexOf(".") + 1), value, target)) {
            try {
                return dumpMapToYaml(yamlToMap, path);
            } catch (IOException e) {
                e.printStackTrace();
                log.error("yaml file update failed !");
                log.error("msg : " + e.getMessage());
                log.error("cause : " + e.getCause());
                return false;
            }
        } else return false;
    }

//        try (FileWriter fileWriter = new FileWriter(path)) {
//            if (setValue(target, key.substring(key.lastIndexOf(".") + 1), value)) {// 这样就不用进去太深了 //if (this.setValue(yamlToMap, key, value)) {
//                Yaml yaml = new Yaml(dumperOptions);
//                yaml.dump(yamlToMap, fileWriter);
//                return true;
//            } else {
//                return false;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            log.error("yaml file update failed !");

//        }
//        return false;

    /**
     * @param key       键
     * @param value     值
     * @param listOrMap 可能是List也可能是Map的对象
     * @return boolean
     * @Author Sennri
     * @Description 采用递归方法向下赋值, 遇到不存在的Key路径将强制存在生成路径对应的对象
     * @Date 2020/11/2 10:59
     * @ParamList:
     */
    public static boolean insertValueToObject(String key, Object value, Object listOrMap) throws Exception {
        if (key.isEmpty()) //因为这个函数会复用，所以空判断在这里进行即可
            return false;
        String[] keys = key.split("[.]");
        int len = keys.length;
        Object temp = listOrMap;

        for (int i = 0; i < len; i++) { //
            if ((getValue(keys[i], temp)) != null) {
                temp = getValue(keys[i], temp);
            } else if (i <= len - 2) {
                try {
                    new Integer(keys[i + 1]); // 若当前键的下一个键为数字，意味着当前本该得到却没有提取到的temp实际上是一个ArrayList
                    ArrayList<Object> list = new ArrayList<>();
                    setValue(keys[i], list, temp);
                    temp = list;
                } catch (Exception e) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    setValue(keys[i], map, temp);
                    temp = map;
                }
            }
        }
        return setValue(keys[keys.length - 1], value, temp); //写入键值,这个真的会报失败吗？
    }


    /**
     * @param key        完整的文件位置（可以省略路径）
     * @param value      打算设置的值
     * @param yamlToMap  转换成LinkedHashMap的yaml文件
     * @param outputPath 文件输出路径（如果有的话）
     * @return boolean
     * @Author Sennri
     * @Description 向下不断使用getValue，得不到时则判断下一个键是否位数字，若为数字创建ArrayList，若为字段创建LinkedHashMap
     * @Date 2020/11/1 22:56
     * @ParamList:
     */
    public static boolean insertYaml(String key, Object value, Map<String, Object> yamlToMap, String outputPath) throws Exception {
        if (insertValueToObject(key, value, yamlToMap)) {
            try {
                return dumpMapToYaml(yamlToMap, outputPath); //这个真的会报失败吗？
            } catch (IOException e) {
                e.printStackTrace();
                log.error("Yaml file insert failed!");
                return false;
            }
        } else {
            return false;
        }
    }


    /**
     * @param key
     * @param value
     * @param inputPath
     * @param outputPath
     * @return boolean
     * @Author Sennri
     * @Description 重载形式二
     * @Date 2020/11/1 22:55
     * @ParamList:
     */
    public static boolean insertYaml(String key, Object value, String inputPath, String outputPath) throws Exception {
        Map<String, Object> yamlToMap = getYamlToMap(inputPath);
        return insertYaml(key, value, yamlToMap, outputPath);
    }

    /**
     * @param key
     * @param value
     * @param inputPath
     * @return boolean
     * @Author Sennri
     * @Description
     * @Date 2020/11/1 22:54
     * @ParamList:
     */
    public static boolean insertYaml(String key, Object value, String inputPath) throws Exception {
        Map<String, Object> yamlToMap = getYamlToMap(inputPath);
        return insertYaml(key, value, yamlToMap, inputPath);
    }


    public static boolean removeListOrMapContent(String key, Object listOrMap) throws Exception {
        if (null == listOrMap) {
            return false;
        }
        // 只返回倒数第二级
        Object target = getValue(key.substring(0, key.lastIndexOf(".")), listOrMap); //上一级map
        if (target == null) {
            throw new Exception("There is no such a map. Please checkout the key.");
        } else {
            if (target instanceof Map)
                ((Map) target).remove(key.substring(key.lastIndexOf(".") + 1));
            else if (target instanceof List)
                ((List) target).remove(Integer.parseInt(key.substring(key.lastIndexOf(".") + 1)));
            else
                throw new Exception("Error: target must be Map-type or List-type!");
        }
        return true;
    }

    /**
     * @param key
     * @param inputYamlName
     * @param outputYamlName
     * @return boolean
     * @Author Sennri
     * @Description
     * @Date 2020/11/1 23:00
     * @ParamList:
     */
    public static boolean removeYamlContent(String key, String inputYamlName, String outputYamlName) throws Exception {
        Map<String, Object> yamlToMap = getYamlToMap(inputYamlName);
        if (yamlToMap == null) {
            return false;
        }
        if (!removeListOrMapContent(key, yamlToMap)) {
            return false;
        }
        try {
            return dumpMapToYaml(yamlToMap, outputYamlName);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Failed to output the content to file!");
            return false;
        }
    }

    public static boolean removeYamlContent(String key, String yamlName) throws Exception {
        return removeYamlContent(key, yamlName, yamlName);
    }

    /*
    public static void main(String[] args) throws Exception {
        log.debug("12345");
        YamlEditor configs = new YamlEditor();
        Map<String, Object> yamlToMap = configs.getYamlToMap("templates/configtx.yaml");
        //System.out.println(yamlToMap);
        boolean b = configs.updateYaml("Organizations.0.Name", "OrdererMSP", "templates/configtx.yaml");
        System.out.println(b);
        System.out.println(configs.getYamlToMap("templates/configtx.yaml"));
    }
     */
}


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