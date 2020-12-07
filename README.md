# es_plug
elasticsearch字符串匹配插件

# Elasticsearch Script介绍

当Elasticsearch的搜索功能不满足业务需要时，使用Script开发更高级的搜索。Elasticsearch支持内建的脚本也支持java编写的插件。Elasticsearch技术栈中的新技术很多都是以插件的形式集成到Elasticsearch中的。

![20200812163518](E:/项目课题/用户身份信息/picture/20200812163518.png)

# 插件开发&测试

本插件开发主要参考了：https://www.elastic.co/guide/en/elasticsearch/reference/7.8/modules-scripting-engine.html 和 https://www.elastic.co/guide/en/elasticsearch/plugins/7.8/plugin-authors.html#_plugin_descriptor_file

高级搜索功能的实现代码是在 ExpertScriptPlugin.java中的 public double execute(ExplanationHolder explanation) 

开发完成后，填写 plugin-descriptor.properties 文件。

## 插件安装

将打包的插件和运行时所需要的其他jar包，连同描述文件一同打入压缩包。

![20200812165254](E:/项目课题/用户身份信息/picture/20200812165254.png)

**注意：插件对应的JDK版本是11**

在elasticsearch安装目录的bin目录下执行：

```
elasticsearch-plugin.bat install file:///${zip_path}
```

安装成功的打印信息如下

```
-> Installing file:///e:/es_plug.zip
-> Downloading file:///e:/es_plug.zip
[=================================================] 100%??
-> Installed my_plugin
```

然后再执行

```
elasticsearch.bat
```

从控制台打印的信息中可以发现插件已经加载

![20200812165850](E:/项目课题/用户身份信息/picture/20200812165850.png)

插件卸载使用

```
elasticsearch-plugin.bat remove ${插件名字}（可通过elasticsearch-plugin.bat list查看）
```



## 测试DSL

```
POST ${index}/_search
{
  "query": {
    "function_score": {
      "script_score": {
        "script": {
          "source": "name_img_similarity",
          "lang": "name_img_scripts",
          "params": {
            "name": "zhang2019"
          }
        }
      }
    }
  }
}
```

