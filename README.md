CodeStaging
====
一个适合大懒比使用的简易代码生成工具（Java），同时也是一个完整的SpringBoot项目！
* 内带一个可视化界面和一个通用模板
* 可以添加自定义模板和自定义占位符
* 通过界面选择模板生成独立的子模块
* 自动读取模块数据库配置生成实体类(2024/6/7)

环境
----------
IDEA, JDK1.8

使用
----------
* 运行项目: 启动 CodeStagingApplication.java
* 生成子模块: 启动 ModuleGenerator.java
```
选择'系统生成' -> 选择系统模板 -> 填写唯一标识(如:Zoo)和版本号(如:1.0) -> 点击生成
 -> 重新启动项目 -> 访问 http://localhost:8088/Zoo
```
* 管理子模块
```
启动工具界面后，选择系统管理，通过穿梭框管理模块
通过工具卸载子模块后可直接在IDEA中删除子模块
```
* 实体类生成
```
启动工具界面后，选择实体类生成，选择模块
选择后会自动读取模块里的application-模块名-local.yml中的数据库配置
点击生成后，会在模块代码包中生成entity目录和各实体类
```

演示
----------
TODO
