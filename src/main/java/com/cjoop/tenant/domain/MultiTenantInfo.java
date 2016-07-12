package com.cjoop.tenant.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 多租户信息
 * @author 陈均
 *
 */
@Entity
@Table(name="multi_tenant_info")
public class MultiTenantInfo implements Serializable{
	private static final long serialVersionUID = 1L;
	public static final String TENANT_TYPE_DATABASE = "database";
	public static final String TENANT_TYPE_SCHEMA = "schema";
	public static final String TENANT_TYPE_DISCRIMINATOR = "discriminator";
	
	/**
	 * 主键uuid
	 */
	@Id
	protected String id;
	
	/**
	 * 租户类型(Database,schema,discriminator)
	 */
	protected String tenantType;
	/**
	 * 连接地址
	 */
	protected String url;
	/**
	 * 数据库连接账号
	 */
	protected String userName;
	/**
	 * 数据库连接密码(暂未考虑加密)
	 */
	protected String password;
	/**
	 * 是否已经初始化
	 */
	protected boolean initialize;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTenantType() {
		return tenantType;
	}
	public void setTenantType(String tenantType) {
		this.tenantType = tenantType;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public boolean isInitialize() {
		return initialize;
	}
	public void setInitialize(boolean initialize) {
		this.initialize = initialize;
	}
}
