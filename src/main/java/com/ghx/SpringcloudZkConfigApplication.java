package com.ghx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//支持多个配置类
@EnableConfigurationProperties({com.ghx.properties.PropertiesBean.class})
@RestController
@SpringBootApplication
public class SpringcloudZkConfigApplication {
	
	
	//
	@Value("${dev.myid}")
	private String myid;

	public static void main(String[] args) {
		SpringApplication.run(SpringcloudZkConfigApplication.class, args);
	}
	
	@RequestMapping("gg")
	public String gg() {
		return myid;
	}
}
