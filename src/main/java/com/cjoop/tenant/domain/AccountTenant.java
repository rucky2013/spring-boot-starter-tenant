package com.cjoop.tenant.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 账号租户关系信息
 * 
 * @author 陈均
 *
 */
@Entity
@Table(name = "account_tenant")
public class AccountTenant implements Serializable {

	private static final long serialVersionUID = 663777764993728961L;
	/**
	 * 对应账户
	 */
	protected String account;
	/**
	 * 对应租户
	 */
	protected String tenant;

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

}
