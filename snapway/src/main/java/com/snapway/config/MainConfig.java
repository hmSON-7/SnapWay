package com.snapway.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.snapway.model.mapper.MemberMapper;

@Configuration
/* 
 * 추가로 사용할 application.properties가 있다면 아래 코드를 주석해제하고 사용하세요.
 * 경로 예시 ---> C:\Users\yohan\second.properties
 */
//@PropertySource("")
@MapperScan(basePackageClasses = {MemberMapper.class})
public class MainConfig {

}
