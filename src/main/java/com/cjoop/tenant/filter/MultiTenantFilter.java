package com.cjoop.tenant.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.util.WebUtils;

import com.cjoop.tenant.config.MultiTenantProperties;
import com.cjoop.tenant.dao.MultiTenantInfoDao;
import com.cjoop.tenant.domain.MultiTenantInfo;

/**
 * 多租户过滤器，主要在登陆的时候根据用户输入的用户名确定租户信息
 * 
 * @author 陈均
 *
 */
public class MultiTenantFilter extends GenericFilterBean {
	/**
	 * 会话租户身份常量
	 */
	public static final String PLATFORM_TENANT_IDENTIFIER_KEY = "PLATFORM_TENANT_IDENTIFIER";
	/**
	 * 登陆表单字段名称
	 */
	public static final String SPRING_SECURITY_FORM_USERNAME_KEY = "username";
	private MultiTenantProperties multiTenantProperties;
	private MultiTenantInfoDao multiTenantInfoDao;

	public MultiTenantFilter() {

	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		String path = request.getRequestURI();
		if (multiTenantProperties.getLoginUrl().equals(path)) {
			String username = request.getParameter(SPRING_SECURITY_FORM_USERNAME_KEY);
			MultiTenantInfo multiTenantInfo = multiTenantInfoDao.findTenantByAccount(username);
			if (multiTenantInfo != null) {
				WebUtils.setSessionAttribute(request, PLATFORM_TENANT_IDENTIFIER_KEY, multiTenantInfo.getId());
			}
		}
		filterChain.doFilter(request, response);
	}

	public MultiTenantProperties getMultiTenantProperties() {
		return multiTenantProperties;
	}

	public void setMultiTenantProperties(MultiTenantProperties multiTenantProperties) {
		this.multiTenantProperties = multiTenantProperties;
	}

	public MultiTenantInfoDao getMultiTenantInfoDao() {
		return multiTenantInfoDao;
	}

	public void setMultiTenantInfoDao(MultiTenantInfoDao multiTenantInfoDao) {
		this.multiTenantInfoDao = multiTenantInfoDao;
	}

}
