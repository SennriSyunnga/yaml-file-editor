package com.fidt.yamleditor;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class TestEditor {
    @Test
    public void testFabCar() throws Exception {
        //EnrollAdmin.main(null);
        //RegisterUser.main(null);
        //YamlEditor.main(null);
        YamlEditor configs = new YamlEditor();
        Map<String, Object> yamlToMap = configs.getYamlToMap("templates/configtx.yaml");
        //System.out.println(yamlToMap);
        boolean b = configs.updateYaml("Organizations.0.Name", "OrdererMSP-FIDT", "templates/configtx.yaml");
        System.out.println(b);
        System.out.println(configs.getYamlToMap("templates/configtx.yaml"));
    }
    @Test
    public void testYamlGetValue(){
        LinkedHashMap<String,Object> map = new LinkedHashMap();
        map.put("1",1);
        map.put("2",2);
        LinkedHashMap<String,Object> map2 = new LinkedHashMap();
        map.put("3",map2);
        if (YamlEditor.getValue("3",map) instanceof Map){
            Map map3 = (Map) YamlEditor.getValue("3",map);
            map3.put("9",1);
        }
        return;
    }
    @Test
    public void testYamlGetValue2(){
        LinkedHashMap<String,Object> map = new LinkedHashMap();
        map.put("1",1);
        LinkedHashMap<String,Object> map2 = new LinkedHashMap();
        map.put("3",map2);
        map2.put("1",2);
        if (YamlEditor.getValue("3",map) instanceof Map){
            Map map3 = (Map) YamlEditor.getValue("3",map);
            map3.put("1",5);
        }
        return;
    }
}
