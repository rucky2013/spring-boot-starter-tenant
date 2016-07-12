package com.cjoop.tenant.hibernate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.AbstractDataSourceBasedMultiTenantConnectionProviderImpl;
import org.hibernate.service.spi.Stoppable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;
import com.cjoop.tenant.dao.MultiTenantInfoDao;
import com.cjoop.tenant.domain.MultiTenantInfo;

/**
 * 数据源多租户连接实现
 * 
 * @author 陈均
 *
 */
@Component("dataSourceMultiTenantConnectionProvider")
public class DataSourceMultiTenantConnectionProviderImpl
		extends AbstractDataSourceBasedMultiTenantConnectionProviderImpl implements Stoppable,ApplicationContextAware {
	private static final long serialVersionUID = 1L;
	/**
	 * 默认数据源，该数据源存放有租户信息
	 */
	@Autowired
	private DataSource defDataSource;
	private ApplicationContext applicationContext;
	private MultiTenantInfoDao multiTenantInfoDao;
	private Map<String, DataSource> dataSourceMap;

	public DataSourceMultiTenantConnectionProviderImpl() {
		
	}

	@Override
	protected DataSource selectAnyDataSource() {
		return defDataSource;
	}

	@Override
	protected DataSource selectDataSource(String tenantIdentifier) {
		DataSource dataSource = dataSourceMap().get(tenantIdentifier);
		if (dataSource == null) {
			multiTenantInfoDao = applicationContext.getBean(MultiTenantInfoDao.class);
			MultiTenantInfo multiTenantInfo = multiTenantInfoDao.findTenantByTenant(tenantIdentifier);
			if (multiTenantInfo != null) {
				DruidDataSource druidDataSource = new DruidDataSource();
				druidDataSource.setUrl(multiTenantInfo.getUrl());
				druidDataSource.setUsername(multiTenantInfo.getUserName());
				druidDataSource.setPassword(multiTenantInfo.getPassword());
				dataSourceMap().put(tenantIdentifier, druidDataSource);
				return druidDataSource;
			}
		}
		return defDataSource;
	}

	private Map<String, DataSource> dataSourceMap() {
		if (dataSourceMap == null) {
			dataSourceMap = new ConcurrentHashMap<String, DataSource>();
		}
		return dataSourceMap;
	}

	@Override
	public void stop() {
		if (dataSourceMap != null) {
			dataSourceMap.clear();
			dataSourceMap = null;
		}
	}

	public MultiTenantInfoDao getMultiTenantInfoDao() {
		return multiTenantInfoDao;
	}

	public void setMultiTenantInfoDao(MultiTenantInfoDao multiTenantInfoDao) {
		this.multiTenantInfoDao = multiTenantInfoDao;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
