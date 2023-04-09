package com.zw.alipaytest;


import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DateType;

import java.util.Collections;

/**
 * mybatis-plus代码生成
 */
public class TestA {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/userdb?serverTimezone=GMT%2B8", "root", "888888")
                .globalConfig(builder -> {
                    builder.author("zzw") // 设置作者 baomidou 默认值:作者
                            .enableSwagger() // 开启 swagger 模式 默认值:false
                            .fileOverride() // 覆盖已生成文件 默认值:false
                            .disableOpenDir()//禁止打开输出目录 默认值:true
                            .commentDate("yyyy-MM-dd")// 注释日期
                            .dateType(DateType.ONLY_DATE)//定义生成的实体类中日期类型 DateType.ONLY_DATE 默认值: DateType.TIME_PACK
                            .outputDir(System.getProperty("user.dir") + "/src/main/java"); // 指定输出目录 /opt/baomidou/ 默认值: windows:D:// linux or mac : /tmp
                })
                .packageConfig(builder -> {
                    builder.parent("com.zw.alipaytest") // 父包模块名 默认值:com.baomidou
                            .controller("controller")//Controller 包名 默认值:controller
                            .entity("domain")//Entity 包名 默认值:entity
                            .service("service")//Service 包名 默认值:service
                            .mapper("mapper")//Mapper 包名 默认值:mapper
                            .pathInfo(Collections.singletonMap(OutputFile.xml,System.getProperty("user.dir")+ "/src/main/java/com/zw/alipaytest/mapper")); // 设置mapper.xml存放路径
                    //默认存放在mapper的xml下
                })
                .strategyConfig(builder -> {
                    builder.addInclude("order_t") // 设置需要生成的表名 可边长参数“user”, “user1”
                            .entityBuilder()// 实体类策略配置
                            .enableLombok() //开启lombok
                            .logicDeleteColumnName("delete_time")// 说明逻辑删除是哪个字段
                            .enableTableFieldAnnotation()// 属性加上注解说明
                            .mapperBuilder()// mapper策略配置
                            .formatMapperFileName("%sMapper")
                            .enableMapperAnnotation()//@mapper注解开启
                            .formatXmlFileName("%sMapper");
                }).execute();
    }
}
