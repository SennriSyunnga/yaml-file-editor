package com.fidt.yamleditor;
import org.junit.Test;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestEditor {
    protected static Log log = LogFactory.getLog(TestEditor.class);

    @Test
    public void test() throws Exception {
        LinkedHashMap map = new LinkedHashMap();
        Object object=YamlEditor.getValue("s",map);//取空值测试
        YamlEditor.insertValueToObject("Organizations.0.Name","org1",map); //创建新路径测试
        log.info(map);
        YamlEditor.insertValueToObject("Organizations.1.Name","org2",map);
        log.info(map);
        YamlEditor.insertValueToObject("Organizations.3.Name","org3",map); //index out of range时会自动加在最末尾
        log.info(map);
        YamlEditor.insertValueToObject("Organizations.0.ports.0","172.21.18.41",map);
        log.info(map);
        YamlEditor.setValue("Organizations.0.Name","FIDT",map); //改值测试
        YamlEditor.setValue("Organizations.0.ports.1","172.21.18.42",map); //setValue添加key value对测试
        log.info(YamlEditor.getValue("Organizations.0.ports.1",map));
        YamlEditor.removeListOrMapContent("Organizations.1",map);
        log.info(map);
        YamlEditor.dumpMapToYaml(map,"test.yaml");
        Map temp = YamlEditor.getYamlToMap("test.yaml");
        log.info(temp);
        return;
    }

    @Test
    public void testUpdateYaml() throws Exception {
        YamlEditor configs = new YamlEditor();
        Map<String, Object> yamlToMap = configs.getYamlToMap("templates/configtx.yaml");
        System.out.println(yamlToMap);
        boolean b = configs.updateYaml("Organizations.0.Name", "Orderer", "templates/configtx.yaml");
        System.out.println(b);
        System.out.println(yamlToMap);
        System.out.println(configs.getYamlToMap("templates/configtx.yaml"));
    }

    @Test
    public void testInsertYaml() throws Exception {
        LinkedHashMap map = new LinkedHashMap();
        YamlEditor.insertValueToObject("Organizations.0.Name","org1",map);
        YamlEditor.insertValueToObject("Organizations.1.Name","org2",map);
        YamlEditor.insertValueToObject("Organizations.3.Name","org3",map);
        YamlEditor.insertValueToObject("Organizations.0.ports.0","172.21.18.41",map);
    }

    @Test
    public void testYamlSetValue() throws Exception {
        LinkedHashMap map = new LinkedHashMap();
        YamlEditor.insertValueToObject("Organizations.0.Name","org1",map);
        YamlEditor.insertValueToObject("Organizations.1.Name","org2",map);
        YamlEditor.insertValueToObject("Organizations.3.Name","org3",map);
        YamlEditor.insertValueToObject("Organizations.0.ports.0","172.21.18.41",map);
        YamlEditor.setValue("Organizations.0.Name","FIDT",map);
        YamlEditor.setValue("Organizations.0.ports.1","172.21.18.42",map);
    }

    @Test
    public void testYamlGetValue() throws Exception {
        LinkedHashMap map = new LinkedHashMap();
        YamlEditor.insertValueToObject("Organizations.0.Name","org1",map);
        YamlEditor.insertValueToObject("Organizations.1.Name","org2",map);
        YamlEditor.insertValueToObject("Organizations.3.Name","org3",map);
        YamlEditor.insertValueToObject("Organizations.0.ports.0","172.21.18.41",map);
        YamlEditor.setValue("Organizations.0.Name","FIDT",map);
        YamlEditor.setValue("Organizations.0.ports.1","172.21.18.42",map);
        log.info(YamlEditor.getValue("Organizations.0.ports.1",map));
    }
    @Test
    public void testYamlGetValue2() throws Exception {
        LinkedHashMap<String,Object> map = new LinkedHashMap();
        map.put("1",1);
        LinkedHashMap<String,Object> map2 = new LinkedHashMap();
        map.put("2",map2);

        ArrayList<Integer> list = new ArrayList<>();
        list.add(2);
        map.put("4",list);
        map2.put("1",2);
        Object o = YamlEditor.getValue("4.0",map);
        if (YamlEditor.getValue("4.0",map) instanceof Map){
            Map map3 = (Map) YamlEditor.getValue("3",map);
            map3.put("1",5);
        }
        return;
    }
}
