package com.ghx.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

//可以动态修改值,不需要重启
@ConfigurationProperties(prefix = "com.xxx")
public class PropertiesBean {

	
}
