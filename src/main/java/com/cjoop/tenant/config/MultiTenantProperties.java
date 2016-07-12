package com.cjoop.tenant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多租户配置信息,主要是针对共享表的配置
 * 
 * @author 陈均
 *
 */
@ConfigurationProperties(prefix = "multi.tenant")
public class MultiTenantProperties {

	/**
	 * 租户字段名,默认值tenant_id,不能够修改为其他名字，还有些代码没和这里打通
	 */
	protected String fieldName = "tenant_id";
	/**
	 * 租户登陆地址，默认采用SpringSecurity登陆地址
	 */
	protected String loginUrl = "/j_spring_security_check";

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getLoginUrl() {
		return loginUrl;
	}

	public void setLoginUrl(String loginUrl) {
		this.loginUrl = loginUrl;
	}

}
