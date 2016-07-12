package com.cjoop.tenant.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

import com.cjoop.tenant.filter.MultiTenantFilter;

public class MultiTenantUtil {
	/**
	 * 系统默认租户id值
	 */
	public static String DEFAULT_TENANT_ID = "default_tenant_id";
	
	/**
	 * 获取session中租户id信息,如果没有提供租户id信息，返回默认系统默认租户
	 * @return tenantId
	 */
	public static String getTenantId(){
		ServletRequestAttributes requestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
		if(requestAttributes!=null){
			Object tenantIdentifier = WebUtils.getSessionAttribute(requestAttributes.getRequest(), MultiTenantFilter.PLATFORM_TENANT_IDENTIFIER_KEY);
			if (tenantIdentifier != null) {
				return tenantIdentifier.toString();
			}
		}
		return DEFAULT_TENANT_ID;
	}
	
	
}
