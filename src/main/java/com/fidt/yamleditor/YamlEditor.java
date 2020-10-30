package com.fidt.yamleditor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;

import static java.lang.String.valueOf;


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
     * @Param
     * @Author Sennri
     * @Description 将yaml配置文件转化成map
     * @Date 2020/10/29 16:18
     **/

    public static Map<String, Object> getYamlToMap(String fileName) {
        LinkedHashMap<String, Object> yamls = new LinkedHashMap<>();
        Yaml yaml = new Yaml();
        // @Cleanup InputStream in = YamlEditor.class.getClassLoader().getResourceAsStream(fileName); // 这里舍弃了@写法
        try (InputStream in = YamlEditor.class.getClassLoader().getResourceAsStream(fileName);) {
            yamls = yaml.loadAs(in, LinkedHashMap.class); // 这里应该是有问题的？
        } catch (Exception e) {
            //log.error("{} load failed !!!", fileName);
        }
        return yamls;
    }


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

    /**
     * @param key
     * @param target
     * @return java.lang.Object
     * @Author Sennri
     * @Description 泛型化上面两个函数
     * @Date 2020/10/29 23:05
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
                return ((List) target).get(Integer.parseInt(key));
            }
        } else if (target instanceof Map) {
            if (key.contains(".")) {
                String[] keys = key.split("[.]");
                Object object = ((Map) target).get(keys[0]);
                if (object instanceof Map || object instanceof List) {
                    return getValue(key.substring(key.indexOf(".") + 1), object);
                } else {
                    return null;
                }
            } else {
                return ((Map) target).get(key);
            }
        } else {
            throw new Exception("Collection type error");
        }
    }


    // TODO: 2020/10/23 这里应该有问题,并不能解决如果下面是一个数组的情况，但是目前应该还没遇到这种情况

    /**
     * @param key
     * @param value
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Param
     * @Author Sennri
     * @Description 使用递归的方式设置map中的值，仅适合单一属性 key的格式: "server.port"
     * @Date 2020/10/29 15:42
     **/
    public Map<String, Object> generateMap(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] keys = key.split("[.]");
        result.put(keys[keys.length - 1], value);
        if (keys.length > 1) {
            return generateMap(key.substring(0, key.lastIndexOf(".")), result);
        }
        return result;
    }

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



    public boolean setValue(Object target, String key, Object value) throws Exception {
        if (!key.contains(".")) { //说明到达最底的键
            if (target instanceof Map) {
                ((Map) target).put(key, value); //即便没有这个值，也会加上去——有一点风险，感觉不建议这么做。会因为错误操作而改变原有结构。
            } else if (target instanceof List) {
                ((List) target).set(Integer.parseInt(key), value);
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
                return setValue(object, key.substring(key.indexOf(".") + 1), value);
            }
        }
    }

    /**
     * 修改yaml中属性的值
     *
     * @param key      key是properties的方式： aaa.bbb.ccc (key不存在不修改)
     * @param value    新的属性值 （新属性值和旧属性值一样，不修改）
     * @param yamlName
     * @return true 修改成功，false 修改失败。
     */
    public boolean updateYaml(String key, @Nullable Object value, String yamlName) throws Exception {
        Map<String, Object> yamlToMap = getYamlToMap(yamlName);
        // getYamlToMap 返回的是Object更合适吧？
        if (null == yamlToMap) {
            return false;
        }
        // 只返回倒数第二级
        Object target = getValue(key.substring(0, key.lastIndexOf(".")), yamlToMap); //上一级map
        Object oldVal = getValue(key.substring(key.lastIndexOf(".") + 1), target); // 对上一级map取key值，得到value

        // 未找到key 不修改
        if (null == oldVal) {
            //log.error("{} key is not found", key);
            return false;
        }

        // TODO: 2020/10/29 显然这个判断不了oldVal是可变对象的情况，例如，一个map，只能判断一下非可变类型，例如，string
        //新旧值一样 不修改
        if (value.equals(oldVal)) {
            log.debug("New Value equals to old Value, newVal, please checkout the value you want to update.");
            return false;
        }

        Yaml yaml = new Yaml(dumperOptions);
        String path = Objects.requireNonNull(this.getClass().getClassLoader().getResource(yamlName)).getPath();
        //String path = this.getClass().getClassLoader().getResource(yamlName).getPath();
        try (FileWriter fileWriter = new FileWriter(path)) {
            if (this.setValue(target, key.substring(key.lastIndexOf(".") + 1), value)) {// 这样就不用进去太深了
                //if (this.setValue(yamlToMap, key, value)) {
                yaml.dump(yamlToMap, fileWriter);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("yaml file update failed !");
            //log.error("msg : {} ", e.getMessage());
            //log.error("cause : {} ", e.getCause());
        }
        return false;
    }


    public boolean insertYaml(String key, Object value, Map<String, Object> yamlToMap, String path) {

        Yaml yaml = new Yaml(dumperOptions);
        String[] keys = key.split("[.]");

        int len = keys.length;
        Map temp = yamlToMap;

        for (int i = 0; i < len; i++) {
            if (temp.containsKey(keys[i])) {
                temp = (Map) temp.get(keys[i]);
            } else {
                if (i  <= len - 2) {
                    System.out.println("This map doesn't have the key: " + keys[i]);
                    try {
                        Integer integer = new Integer(keys[i]);
                    }catch (Exception e){
                        Map<String, Object> newMap = new LinkedHashMap<>();
                        newMap.put(keys[len - 1], value);
                    }


                    temp.put(keys[i], newMap);
                } else {
                    temp.put(keys[i], value);
                }
                break;
            }
        }
        try (FileWriter fileWriter = new FileWriter(path)){
            yaml.dump(yamlToMap, fileWriter);
        } catch (Exception e) {
            System.out.println("yaml file insert failed !");
            return false;
        }
        return true;
    }

    public static boolean removeYamlContent(String key, String yamlName) throws Exception {
        Map<String, Object> yamlToMap = getYamlToMap(yamlName);
        if (null == yamlToMap) {
            return false;
        }
        // 只返回倒数第二级
        Object target = getValue(key.substring(0, key.lastIndexOf(".")), yamlToMap); //上一级map
        if (target == null) {
            throw new Exception("There is no such a map. Please checkout the key.");
        } else {
            if (target instanceof Map)
                ((Map) target).remove(key.substring(key.lastIndexOf(".") + 1));
            else if (target instanceof List)
                ((List) target).remove(Integer.parseInt(key.substring(key.lastIndexOf(".") + 1)));
            else
                throw new Exception("Error: target must be Map-type or List-type!");
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        log.debug("12345");
        YamlEditor configs = new YamlEditor();
        Map<String, Object> yamlToMap = configs.getYamlToMap("templates/configtx.yaml");
        //System.out.println(yamlToMap);
        boolean b = configs.updateYaml("Organizations.0.Name", "OrdererMSP", "templates/configtx.yaml");
        System.out.println(b);
        System.out.println(configs.getYamlToMap("templates/configtx.yaml"));
    }
}



