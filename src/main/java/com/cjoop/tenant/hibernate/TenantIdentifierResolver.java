package com.cjoop.tenant.hibernate;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import com.cjoop.tenant.util.MultiTenantUtil;

/**
 * 获取当前租户身份信息
 * 
 * @author 陈均
 *
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

	private static ThreadLocal<String> threadLocal = new ThreadLocal<String>();

	/**
	 * 手动设置租户信息(主要用于测试)
	 * 
	 * @param tenantId
	 *            租户身份
	 */
	public static void setDefaultTenantId(String tenantId) {
		threadLocal.set(tenantId);
	}

	/**
	 * 获取默认的租户信息
	 * 
	 * @return tenantId 租户身份
	 */
	public static String getDefaultTenantId() {
		String defaultTenantId = threadLocal.get();
		if (defaultTenantId == null) {
			threadLocal.set(MultiTenantUtil.DEFAULT_TENANT_ID);
		}
		return threadLocal.get();
	}

	@Override
	public String resolveCurrentTenantIdentifier() {
		String tenantIdentifier = MultiTenantUtil.getTenantId();
		if (tenantIdentifier != null) {
			threadLocal.set(tenantIdentifier);
			return tenantIdentifier;
		}
		return getDefaultTenantId();
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}

}
