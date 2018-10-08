package com.ghx;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//import com.ghx.properties.PropertiesBean;

//@ConfigurationProperties
//支持多个配置类
//@EnableConfigurationProperties({com.ghx.properties.PropertiesBean.class})
@RestController
@SpringBootApplication
//配置修改自动刷新配置属性(注入方式的值不变，仅对@Value有效)
@RefreshScope
public class SpringcloudZkConfigApplication {
	
//	@Autowired
//	PropertiesBean propertiesBean;
	//
	@Value("${myid}")
	private String myid;

	public static void main(String[] args) {
		SpringApplication.run(SpringcloudZkConfigApplication.class, args);
	}
	
	@RequestMapping("gg")
	public String gg() {
//		return propertiesBean.getMyid();
		return myid;
	}
}
