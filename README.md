# 本工具实现了以下功能：
## 文件读写操作相关有：
1.  以LinkedHashMap的形式读入一个yaml文件 getMapFromYaml
2.  将一个LinkedHashMap输出为Yaml文件。（仅包含Map和ArrayList） dumpMapToYaml
3.  更新一个已经存在的yaml文件的特定键值 updateYaml
4.  移除一个已经存在的yaml文件的特定键值 removeYamlContent
5.  根据复核键，向一个yaml文件里直接写入键值，若所在键的“路径”不存在，则创建该路径上所有不存在的对象。 insertYaml （如果路径中arraylist超出index的逻辑需要复核一下）

## 底层可以使用的函数有：

```java
public static boolean insertValueToObject(String key, Object value, Object listOrMap) //增
public static boolean removeListOrMapContent(String key, Object listOrMap) //删
public static Object getValue(String key, Object target) //查
public static boolean setValue(String key, Object value, Object target) //改
```

即对ListOrMap对象的增删查改

其中setValue对最低层级的键若不存在也会赋值，需不需要保留这个特性，暂时搁置。

可以通过TestEditor类进行测试。

## 对于Key和Value的要求：
最下层Value不做要求。复合key用"."隔开，为区分当前对象是数组还是Map，Map的Key值不能为数字
如下面的取千里这个值，需要用的键为：
"Service.Profile.Organization.0.campany"
```yaml
Service：
    Profile：
        Organization：
            -org1：
                company：千里
            -org2：
```

##TODO
不支持map对象内使用整形数字作为键。