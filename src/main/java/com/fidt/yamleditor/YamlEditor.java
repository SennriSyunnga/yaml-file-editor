package com.fidt.yamleditor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.apache.commons.lang.StringUtils;


import org.springframework.lang.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


//@Slf4j
//@Component

public class YamlEditor {
    protected static Log log = LogFactory.getLog(YamlEditor.class);//protected static Logger log = LoggerFactory.getLogger(YamlEditor.class);

    private static final DumperOptions dumperOptions = new DumperOptions();

    static {
        //设置yaml读取方式为块读取
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.PLAIN);
        dumperOptions.setPrettyFlow(false);
    }

    private YamlEditor(){}

    /**
     * @param fileName 默认是resources目录下的yaml文件, 如果yaml文件在resources子目录下，需要加上子目录 比如：conf/config.yaml
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Author Sennri
     * @Description 将yaml配置文件转化成map
     * @Date 2020/11/1 22:48
     * @ParamList:
     */
    public static Map<String, Object> getMapFromYaml(String fileName) throws IOException {
        return getMapFromYaml(Paths.get(fileName));
    }

    /**
     * @Author Sennri
     * @Description 
     * @Date 2020/11/2 23:01
     * @ParamList: 
     * @param path
     * @return java.util.Map<java.lang.String,java.lang.Object>
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
     * @param yamls    map or list, commonly LinkedHashMap
     * @param fileName
     * @return boolean
     * @Author Sennri
     * @Description 将Map存入fileName指定的文件当中
     * @Date 2020/11/1 22:49
     * @ParamList:
     */
    public static boolean dumpMapToYaml(Map<String, Object> yamls, String fileName) throws IOException {
        return dumpMapToYaml(yamls,fileName,false);
    }

    /**
     * @Author
     * @Description 判断：是否连目录一起创建
     * @Date 2020/12/7 15:51
     * @ParamList:
     * @param yamls
     * @param fileName
     * @param isForced
     * @return boolean
     */
    public static boolean dumpMapToYaml(Map<String, Object> yamls, String fileName, boolean isForced) throws IOException {
        return dumpMapToYaml(yamls,Paths.get(fileName),isForced);
    }


    /**
     * @Author
     * @Description 接受URI作为参数写入文件
     * @Date 2020/11/3 11:35
     * @ParamList:
     * @param yamls
     * @param uri
     * @return boolean
     */
    public static boolean dumpMapToYaml(Map<String, Object> yamls, URI uri) throws IOException {
        return dumpMapToYaml(yamls, Paths.get(uri));
    }
    /**
     * @Author
     * @Description
     * @Date 2020/11/3 11:35
     * @ParamList:
     * @param yamls
     * @param path
     * @return boolean
     */
    public static boolean dumpMapToYaml(Map<String, Object> yamls, Path path) throws IOException {
        // 若不存在该文件，则创造文件。该逻辑不会创建不存在的文件夹。
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
     * @Author Sennri
     * @Description
     * @Date 2020/12/7 16:12
     * @ParamList:
     * @param yamls
     * @param path
     * @param isForced 是否强制添加路径（如果路径中文件夹不存在）
     * @return boolean
     */
    public static boolean dumpMapToYaml(Map<String, Object> yamls, Path path, boolean isForced) throws IOException {
        // 若不存在该文件，则创造文件。该逻辑不会创建不存在的文件夹。
        if (Files.exists(path.getParent())) {
            return dumpMapToYaml(yamls, path);
        } else if (isForced){
            Files.createDirectory(path.getParent()); //未经测试，不能保证功能符合预期。
        }
        return dumpMapToYaml(yamls, path);
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
    public static Object getValue(String key, Object target) throws IllegalArgumentException{
        if (target instanceof List) {
            if (key.contains(".")) {
                String[] keys = key.split("[.]");
                Object object = ((List<Object>) target).get(Integer.parseInt(keys[0]));
                if (object instanceof Map || object instanceof List) { //是Map
                    return getValue(key.substring(key.indexOf(".") + 1), object);
                } else {
                    log.error("Key \"" + keys[0] + "\" not found.");
                    return null; // todo
                }
            } else {
                try {
                    return ((List<Object>) target).get(Integer.parseInt(key));
                } catch (IndexOutOfBoundsException e) { // 若超出index，也返回null，这逻辑和Map保持一致
                    e.printStackTrace();
                    return null; // todo
                }
            }
        } else if (target instanceof Map) {
            if (key.contains(".")) {
                String[] keys = key.split("[.]");
                Object object = ((Map<String,Object>) target).get(keys[0]);
                if (object instanceof Map || object instanceof List) {
                    return getValue(key.substring(key.indexOf(".") + 1), object);
                } else {
                    log.error("Key chains error. Cant get the target key.");
                    return null;
                }
            } else {
                return ((Map<String,Object>) target).get(key);
            }
        } else {
            // todo 应该抛出可读性更高的特定异常名
            throw new IllegalArgumentException("Target type do not match. The result should be List or Map, maybe you use wrong key?");
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
     * @param target 需要进行键值修改的对象
     * @param key    目标键
     * @param value  目标值
     * @return boolean
     * @Author Sennri
     * @Description 向target内寻找键值key，若key为复合形式，则一直向下取键，获得中间的value值，若这中间发现某个键在递归途中实际上不存在，则返回false
     * 如果只是最下级的键不存在，会直接put该键。因此也可以用这个函数在最底层进行put操作
     * @Date 2020/11/1 22:45
     * @ParamList:
     */
    //todo 这里增加了
    public static boolean setValue(String key, Object value, Object target) throws Exception {
        if (key.isEmpty()) // 为了复用该函数来进行普通的put操作
            return false;
        if (!key.contains(".")) { //说明到达最底的键
            if (target instanceof Map) {
                ((Map<String,Object>) target).put(key, value); //即便没有这个值，也会加上去——有一点风险，感觉不建议这么做。会因为错误操作而改变原有结构。
            } else if (target instanceof List) {
                int index = Integer.parseInt(key);
                if (0 <= index && index <= ((List<Object>) target).size() - 1) {
                    ((List<Object>) target).set(index, value);
                } else { //若超出既有列表边界，又或者键为负，则为新增。
                    ((List<Object>) target).add(value);
                }
            } else {
                throw new Exception("Error: The type of target is expected as Map or List!");
            }
            return true; // 设置成功
        } else { //未到达最底层
            String[] keys = key.split("[.]");
            Object object;
            if (target instanceof Map) {
                object = ((Map<String,Object>) target).get(keys[0]);
            } else if (target instanceof List)
                object = ((List<Object>) target).get(Integer.parseInt(keys[0]));
            else {
                //todo Exception特化
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
    public static boolean updateYaml(String key, @Nullable Object value, String yamlName) throws Exception {
        Map<String, Object> yamlToMap = getMapFromYaml(yamlName);
        // if the yaml file is empty, then return false.
        if (yamlToMap == null) {
            return false;
        }
        // 返回待取键值所在的Map或者List  //return the Map or List to which the key belong.
        // 这里改为直接抛异常，因为没有这个键。
        Object target = Objects.requireNonNull(getValue(key.substring(0, key.lastIndexOf(".")), yamlToMap));

//        if (target == null){
//            log.error("Key chain error.");
//            return false;
//        }

        // get the old value from target object.
        Object oldValue = getValue(key.substring(key.lastIndexOf(".") + 1), target); // 对上一级map取key值，得到value

        // 未找到key 返回false // key not found, return false.
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

        // 这里限定了从classes文件夹下读取文件，目前注释掉 // String path = Objects.requireNonNull(YamlEditor.class.getClassLoader().getResource(yamlName)).getPath().substring(1);//String path = this.getClass().getClassLoader().getResource(yamlName).getPath();
        if (setValue(key.substring(key.lastIndexOf(".") + 1), value, target)) {
            try {
                return dumpMapToYaml(yamlToMap, Paths.get(yamlName));
            } catch (IOException e) {
                e.printStackTrace();
                log.error("yaml file update failed !");
                log.error("msg : " + e.getMessage());
                log.error("cause : " + e.getCause());
                return false;
            }
        } else return false;
    }

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
        if (key.isEmpty()) //因为这个函数 可能 会复用，所以空判断在这里进行即可？
            return false;
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
                } catch (Exception e) {
                    Map<String, Object> map = new LinkedHashMap<>(); // 插入一个newlinkedhashMap
                    setValue(keys[i], map, temp);
                    temp = map;
                }
            }
        }
        //此时i=len-1，出循环
        return setValue(keys[keys.length - 1], value, temp); //写入键值,这个真的会报失败吗？
    }

    /**
     * @param key        完整的键所在路径
     * @param value      打算设置的值
     * @param yamlToMap  转换成LinkedHashMap的yaml文件
     * @param outputPath 文件输出路径（如果有的话）
     * @return boolean
     * @Author Sennri
     * @Description 强制性赋值。向下不断使用getValue，得不到时则判断下一个键是否位数字，若为数字创建ArrayList，若为字段创建LinkedHashMap
     * @Date 2020/11/1 22:56
     * @ParamList:
     */
    public static boolean insertYaml(String key,
                                     Object value,
                                     Map<String, Object> yamlToMap,
                                     String outputPath)
            throws Exception {
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
     * @param key 键
     * @param value 值
     * @param inputPath 输入路径（含文件名）
     * @param outputPath 输出路径（含文件名）
     * @return boolean
     * @Author Sennri
     * @Description 重载形式二
     * @Date 2020/11/1 22:55
     * @ParamList:
     */
    public static boolean insertYaml(String key,
                                     Object value,
                                     String inputPath,
                                     String outputPath)
            throws Exception {
        Map<String, Object> yamlToMap = getMapFromYaml(inputPath);
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
    public static boolean insertYaml(String key,
                                     Object value,
                                     String inputPath)
            throws Exception {
        Map<String, Object> yamlToMap = getMapFromYaml(inputPath);
        return insertYaml(key, value, yamlToMap, inputPath);
    }

    /**
     * @Author Sennri
     * @Description 从List对象或者Map对象中一处特定键值
     * @Date 2020/11/3 10:59
     * @ParamList:
     * @param key
     * @param listOrMap
     * @return boolean
     */
    public static boolean removeListOrMapContent(String key, Object listOrMap) throws Exception {
        if (listOrMap == null) {
            log.info("List or Map is empty.");
            return false;
        }
        // 只返回倒数第二级
        Object target;
        if (key.contains(".")){
            target = getValue(key.substring(0, key.lastIndexOf(".")), listOrMap); //上一级map——如果没有上一级呢？
        }else{
            target = listOrMap;
        }
        if (target == null) {
            log.error("There is no such a map or list to which the key belong. Please check out the key.");
            return false;
        } else {
            if (target instanceof Map)
                ((Map<String,Object>) target).remove(key.substring(key.lastIndexOf(".") + 1));
            else if (target instanceof List)
                ((List<Object>) target).remove(Integer.parseInt(key.substring(key.lastIndexOf(".") + 1)));
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
     * @Description         适用于生成新文件的情况
     * @Date 2020/11/1 23:00
     * @ParamList:
     */
    public static boolean removeYamlContent(String key, String inputYamlName, String outputYamlName) throws Exception {
        Map<String, Object> yamlToMap = getMapFromYaml(inputYamlName);
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

    /**
     * @Author Sennri
     * @Description     重载之一，用于同一文件的读入输出
     * @Date 2020/11/3 10:30
     * @ParamList:
     * @param key
     * @param yamlName
     * @return boolean
     */
    public static boolean removeYamlContent(String key, String yamlName) throws Exception {
        return removeYamlContent(key, yamlName, yamlName);
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