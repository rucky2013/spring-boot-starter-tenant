package com.cjoop.tenant.domain;

import java.io.Serializable;

import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@Filter(name="tenantFilter",condition="tenant_id = :tenantFilterParam")
@FilterDef(name="tenantFilter",parameters={@ParamDef(name="tenantFilterParam",type="string")})
public class TenantEntity implements Serializable{
	private static final long serialVersionUID = 1L;
	/**
	 * 租户id
	 */
	protected String tenantId;//tenant_id
	
	public String getTenantId() {
		return tenantId;
	}

	/**
	 * 禁止手动调用该方法,掉了也会被覆盖,^o^
	 * @param tenantId 租户id
	 */
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	
}
