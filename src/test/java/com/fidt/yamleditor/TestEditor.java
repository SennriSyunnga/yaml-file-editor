package com.fidt.yamleditor;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
@Slf4j
public class TestEditor {
    static Path testPath;

    static {
        try {
            testPath = Paths.get(TestEditor.class.getClassLoader().getResource("./test.yaml").toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testAll() throws Exception {
        Map<String, Object> map = YamlEditor.getMapFromYaml(testPath);
        YamlEditor yamlEditor = new YamlEditor();
        try{
            yamlEditor.getValue("s", map);//取空值测试
        }catch (NullPointerException e) {
            e.printStackTrace();
        }
        yamlEditor.insertValueToObject("Organizations.0.Name", "org1", map); //创建新路径测试
        log.info("{}",map);
        yamlEditor.insertValueToObject("Organizations.1.Name", "org2", map);
        log.info("{}",map);
        yamlEditor.insertValueToObject("Organizations.3.Name", "org3", map); //index out of range时会自动加在最末尾
        log.info("{}",map);
        yamlEditor.insertValueToObject("Organizations.0.ports.0", "172.21.18.41", map);
        log.info("{}",map);
        yamlEditor.insertValueToObject("Organizations.0.ports.1", "172.21.18.43", map);
        log.info("{}",map);
        yamlEditor.setValue("Organizations.0.Name", "千里", map); //改值测试
        yamlEditor.setValue("Organizations.0.ports.1", "172.21.18.42", map); //setValue添加key value对测试
        log.info("{}",yamlEditor.getValue("Organizations.0.ports.1", map));
        yamlEditor.removeListOrMapContent("Organizations.1", map);
        log.info("{}",map);
        //URI uri = YamlEditor.class.getClassLoader().getResource("templates/test.yaml").toURI();
        Path path2 = Paths.get("testResult.yaml");
        YamlEditor.dumpMapToYaml(map, path2);
        return;
    }

    @Test
    public void testInsertValue() throws Exception {
        Map<String, Object> map1 = YamlEditor.getMapFromYaml(testPath);
        YamlEditor yamlEditor = new YamlEditor();
        //Map map = new LinkedHashMap();
        yamlEditor.insertValueToObject("Organizations.1.Name", "org2", map1);
        log.info("{}",map1);
        yamlEditor.insertValueToObject("Organizations.2.Name", "org3", map1); //index out of range时会自动加在最末尾
        log.info("{}",map1);
        yamlEditor.insertValueToObject("Organizations.0.ports.0", "172.21.18.41", map1);
        log.info("{}",map1);
        yamlEditor.insertValueToObject("Organizations.0.ports.1", "172.21.18.43", map1);
        log.info("{}",yamlEditor.getValue("Organizations.0.ports.1", map1));
        return;
    }


    @Test
    public void testSetValue() throws Exception {
        Map<String, Object> map = YamlEditor.getMapFromYaml(testPath);
        YamlEditor yamlEditor = new YamlEditor();
        //Map map = new LinkedHashMap();
        yamlEditor.setValue("Organizations.0.Name", "千里", map); //改值测试
        yamlEditor.setValue("Organizations.0.ports.1", "172.21.18.42", map); //setValue添加key value对测试
        log.info("{}",yamlEditor.getValue("Organizations.0.ports.1", map));
        return;
    }
    @Test
    public void testSetSupportEscape() throws Exception {
        Map<String, Object> map = YamlEditor.getMapFromYaml(testPath);
        YamlEditor yamlEditor = new YamlEditor();
        //Map map = new LinkedHashMap();
        yamlEditor.supportEscape(true);
        yamlEditor.insertValueToObject("Organizations.4.baidu\\.com", "org2", map);
        Path path2 = Paths.get("testResult.yaml");
        YamlEditor.dumpMapToYaml(map, path2);
        return;
    }

    @Test
    public void testUNESCAPE_PATTERN() throws Exception {
        String s = YamlEditor.ESCAPE_PATTERN.matcher("Organizations.4.baidu\\.com").replaceAll(".");
        return;
    }

    @Test
    public void testInsertValueToObject() throws Exception {
        Map map = YamlEditor.getMapFromYaml("test.yaml");
        YamlEditor yamlEditor = new YamlEditor();
        yamlEditor.insertValueToObject("Organizations.0.Name", "org2", map);
        yamlEditor.insertValueToObject("Organizations.0.Name", "org3", map); //index out of range时会自动加在最末尾
        yamlEditor.insertValueToObject("Organizations.0.ports.0", "172.21.18.41", map);
        yamlEditor.insertValueToObject("Organizations.0.ports.1", "172.21.18.43", map);
    }
}