package com.cjoop.tenant.dao;

import com.cjoop.tenant.domain.AccountTenant;
import com.cjoop.tenant.domain.MultiTenantInfo;

/**
 * 租户信息数据接口
 * @author 陈均
 *
 */
public interface MultiTenantInfoDao{

	/**
	 * 根据账户查找对应租户信息
	 * @param account
	 * @return MultiTenantInfo
	 */
	MultiTenantInfo findTenantByAccount(String account);

	/**
	 * 根据租户id查找对应租户信息
	 * @param tenant
	 * @return MultiTenantInfo
	 */
	MultiTenantInfo findTenantByTenant(String tenant);
	
	/**
	 * 保存账户租户关系信息
	 * @param accountTenant
	 * @return 受影响的数
	 */
	int saveAccountTenant(AccountTenant accountTenant);
	
	/**
	 * 保存多租户信息
	 * @param multiTenantInfo
	 * @return 主键id
	 */
	String saveMultiTenantInfo(MultiTenantInfo multiTenantInfo);

}
