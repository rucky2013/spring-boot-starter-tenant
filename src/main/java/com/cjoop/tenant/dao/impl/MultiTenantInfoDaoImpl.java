package com.cjoop.tenant.dao.impl;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.cjoop.tenant.dao.MultiTenantInfoDao;
import com.cjoop.tenant.domain.AccountTenant;
import com.cjoop.tenant.domain.MultiTenantInfo;

@Repository("multiTenantInfoDao")
public class MultiTenantInfoDaoImpl implements MultiTenantInfoDao {
	
	private static final String INSERT_MULTI_TENANT_INFO_SQL = "insert into multi_tenant_info(id,tenant_type,url,user_name,password,initialize) values(?,?,?,?,?,?)";
	private static final String INSERT_ACCOUNT_TENANT_SQL = "insert into account_tenant(account,tenant) values(?,?)";
	private static final String SELECT_FROM_MULTI_TENANT_INFO_WHERE_ID = "select * from multi_tenant_info where id=?";
	private static final String SELECT_FROM_ACCOUNT_TENANT_WHERE_ACCOUNT = "select * from account_tenant where account=?";
	
	protected Log logger = LogFactory.getLog(getClass());
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Override
	public MultiTenantInfo findTenantByAccount(String account) {
		MultiTenantInfo multiTenantInfo = null;
		try {
			AccountTenant accountTenant = jdbcTemplate.queryForObject(SELECT_FROM_ACCOUNT_TENANT_WHERE_ACCOUNT, new Object[]{account}, AccountTenant.class);
			multiTenantInfo = findTenantByTenant(accountTenant.getTenant());
		} catch (EmptyResultDataAccessException e) {
			//logger.warn("没有对应租户信息:" + account);
		}
		return multiTenantInfo;
	}

	@Override
	public MultiTenantInfo findTenantByTenant(String tenant) {
		MultiTenantInfo multiTenantInfo = null;
		try {
			multiTenantInfo = jdbcTemplate.queryForObject(SELECT_FROM_MULTI_TENANT_INFO_WHERE_ID, new Object[]{tenant}, MultiTenantInfo.class);
		} catch (EmptyResultDataAccessException e) {
			//logger.warn("没有对应租户信息:" + tenant);
		}
		return multiTenantInfo;
	}

	@Override
	public int saveAccountTenant(AccountTenant accountTenant) {
		return jdbcTemplate.update(INSERT_ACCOUNT_TENANT_SQL,accountTenant.getAccount(),accountTenant.getTenant());
	}

	@Override
	public String saveMultiTenantInfo(MultiTenantInfo multiTenantInfo) {
		String id = uuid();
		multiTenantInfo.setId(id);
		jdbcTemplate.update(INSERT_MULTI_TENANT_INFO_SQL,
				multiTenantInfo.getId(),multiTenantInfo.getTenantType(),multiTenantInfo.getUrl(),multiTenantInfo.getUserName(),
				multiTenantInfo.getPassword(),multiTenantInfo.isInitialize());
		return id;
	}
	
	/**
	 * 随机获取一个uuid数据，该数据不带-
	 * @return uuid
	 */
	public static String uuid(){
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

}
