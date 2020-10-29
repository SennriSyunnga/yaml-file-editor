package com.fidt.yamleditor;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;


@Slf4j
@Component
public class YamlEditor {
    protected static Logger log = LoggerFactory.getLogger(YamlEditor.class);
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

    public Map<String, Object> getYamlToMap(String fileName) {
        LinkedHashMap<String, Object> yamls = new LinkedHashMap<>();
        Yaml yaml = new Yaml();
        // @Cleanup InputStream in = YamlEditor.class.getClassLoader().getResourceAsStream(fileName); // 这里舍弃了@写法
        try (InputStream in = YamlEditor.class.getClassLoader().getResourceAsStream(fileName);) {
            yamls = yaml.loadAs(in, LinkedHashMap.class);
        } catch (Exception e) {
            log.error("{} load failed !!!", fileName);
        }
        return yamls;
    }

    //这里有个问题，如果这个object没有名字呢？

    /**
     * @param key     key格式：aaa.bbb.ccc (如果对于下面是数组的情况呢？
     * @param yamlMap
     * @return java.lang.Object
     * @Param
     * @Author Sennri
     * @Description 通过properties的方式获取yaml中的属性值
     * @Date 2020/10/29 15:06
     **/
    public static Object getValue(String key, Map<String, Object> yamlMap) {
        if (key.contains(".")) {
            String[] keys = key.split("[.]");
            Object object = yamlMap.get(keys[0]);
            if (object instanceof Map) {
                return getValue(key.substring(key.indexOf(".") + 1), (Map<String, Object>) object);
            } else if (object instanceof List) {
                return getValue(key.substring(key.indexOf(".") + 1), (List<Object>) object);
            } else {
                return null;
            }
        } else {
            return yamlMap.get(key);
        }
    }

    /**
     * @param key
     * @param yamlList
     * @return java.lang.Object
     * @Param
     * @Author DELL
     * @Description 不支持
     * @Date 2020/10/29 15:22
     **/

    public static Object getValue(String key, List<Object> yamlList) {
        if (key.contains(".")) {
            String[] keys = key.split("[.]");
            Object object = yamlList.get(Integer.parseInt(keys[0]));
            if (object instanceof Map) { //是Map
                return getValue(key.substring(key.indexOf(".") + 1), (Map<String, Object>) object);
            } else if (object instanceof List) { //也可能是链表
                return getValue(key.substring(key.indexOf(".") + 1), (List<Object>) object);
            } else {
                return null;
            }
        } else {
            return yamlList.get(Integer.parseInt(key));
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
    public Map<String, Object> setValue(String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        String[] keys = key.split("[.]");
        int i = keys.length - 1;
        result.put(keys[i], value);
        if (i > 0) {
            return setValue(key.substring(0, key.lastIndexOf(".")), result);
        }
        return result;
    }

    /**
     * @param map
     * @param key
     * @param value
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @Param
     * @Author DELL
     * @Description 这个没有考虑有list的情况
     * @Date 2020/10/29 15:40
     **/


    public boolean setValue(Map<String, Object> map, String key, Object value) {
        String[] keys = key.split("[.]");
        Map temp = map;
        for (int i = 0; i < keys.length - 1; i++) {
            if (temp.containsKey(keys[i])) {
                temp = (Map) temp.get(keys[i]);
            } else {
                return false;
            }
        }
        temp.put(keys[keys.length - 1], value);//即便没有这个值
        return true; // 设置成功
    }

    public boolean setValue(Collection map, String key, Object value) {
        String[] keys = key.split("[.]");
        Collection temp = map;
        for (int i = 0; i < keys.length - 1; i++) {
            if (temp.containsKey(keys[i])) {
                temp = (Map) temp.get(keys[i]);
            } else {
                return false;
            }
        }
        temp.put(keys[keys.length - 1], value);//即便没有这个值
        return true; // 设置成功
    }




    /**
     * 修改yaml中属性的值
     *
     * @param key      key是properties的方式： aaa.bbb.ccc (key不存在不修改)
     * @param value    新的属性值 （新属性值和旧属性值一样，不修改）
     * @param yamlName
     * @return true 修改成功，false 修改失败。
     */
    public boolean updateYaml(String key, @Nullable Object value, String yamlName) {

        Map<String, Object> yamlToMap = this.getYamlToMap(yamlName);
        if (null == yamlToMap) {
            return false;
        }

        if
        // 如果getValue不返回最高一级，只返回倒数第二级，然后直接对这个Map setValue不好吗？
        Object oldVal = getValue(key, yamlToMap);

        // 未找到key 不修改
        if (null == oldVal) {
            log.error("{} key is not found", key);
            return false;
        }

        // TODO: 2020/10/29 显然这个判断不了oldVal是对象的情况，只能判断一下非可变类型
        //新旧值一样 不修改
        if (value.equals(oldVal)) {
            log.info("newVal equals oldVal, newVal: {} , oldVal: {}", value, oldVal);
            return false;
        }

        Yaml yaml = new Yaml(dumperOptions);
        String path = this.getClass().getClassLoader().getResource(yamlName).getPath();
        try {
            if (this.setValue(yamlToMap, key, value)) {
                yaml.dump(yamlToMap, new FileWriter(path));
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error("yaml file update failed !");
            log.error("msg : {} ", e.getMessage());
            log.error("cause : {} ", e.getCause());
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
                //该处对yaml的层级需要有判断，可能有问题
                if (i + 2 == len) {
                    Map<String, Object> newMap = new HashMap<>();
                    newMap.put(keys[len - 1], value);
                    temp.put(keys[i], newMap);
                    break;
                } else {
                    temp.put(keys[i], value);
                    break;
                }
            }
        }
        try {
            yaml.dump(yamlToMap, new FileWriter(path));
        } catch (Exception e) {
            System.out.println("yaml file insert failed !");
            return false;
        }
        return true;
    }


//    public static void main(String[] args) {
//        YamlEditor configs = new YamlEditor();
//        Map<String, Object> yamlToMap = configs.getYamlToMap("templates/configtx.yaml");
//        //System.out.println(yamlToMap);
//        boolean b = configs.updateYaml("Organizations.0.Name", "OrdererMSP-FIDT", "templates/configtx.yaml");
//        System.out.println(b);
//        System.out.println(configs.getYamlToMap("templates/configtx.yaml"));
//    }
}



