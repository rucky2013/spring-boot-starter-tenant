package com.cjoop.tenant.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringJtaPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jndi.JndiLocatorDelegate;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.ClassUtils;

import com.cjoop.tenant.hibernate.DataSourceMultiTenantConnectionProviderImpl;

/**
 * 多租户自动配置信息
 * @author 陈均
 *
 */
@Configuration("multiTenantAutoConfiguration")
@EnableConfigurationProperties({ MultiTenantProperties.class })
public class MultiTenantAutoConfiguration implements BeanFactoryAware{
	private static final String JTA_PLATFORM = "hibernate.transaction.jta.platform";
	private static final String[] NO_PACKAGES = new String[0];
	private static final Log logger = LogFactory
			.getLog(MultiTenantAutoConfiguration.class);
	/**
	 * {@code NoJtaPlatform} implementations for various Hibernate versions.
	 */
	private static final String[] NO_JTA_PLATFORM_CLASSES = {
			"org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform",
			"org.hibernate.service.jta.platform.internal.NoJtaPlatform" };
	/**
	 * {@code WebSphereExtendedJtaPlatform} implementations for various Hibernate
	 * versions.
	 */
	private static final String[] WEBSPHERE_JTA_PLATFORM_CLASSES = {
			"org.hibernate.engine.transaction.jta.platform.internal.WebSphereExtendedJtaPlatform",
			"org.hibernate.service.jta.platform.internal.WebSphereExtendedJtaPlatform", };
	private ConfigurableListableBeanFactory beanFactory;
	@Autowired
	private JpaProperties jpaProperties;
	@Autowired
	private DataSource dataSource;
	@Autowired(required = false)
	private JtaTransactionManager jtaTransactionManager;
	@Autowired
	@Qualifier("dataSourceMultiTenantConnectionProvider")
	private MultiTenantConnectionProvider dataSourceMultiTenantConnectionProvider;
	
	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
	}
	
	@Bean
	@Primary
	@ConditionalOnMissingBean({ LocalContainerEntityManagerFactoryBean.class,
			EntityManagerFactory.class })
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			EntityManagerFactoryBuilder factoryBuilder) {
		Map<String, Object> vendorProperties = getVendorProperties();
		customizeVendorProperties(vendorProperties);
		return factoryBuilder.dataSource(this.dataSource).packages(getPackagesToScan())
				.properties(vendorProperties).jta(isJta()).build();
	}
	
	protected Map<String, Object> getVendorProperties() {
		Map<String, Object> vendorProperties = new LinkedHashMap<String, Object>();
		vendorProperties.putAll(this.jpaProperties.getHibernateProperties(this.dataSource));
		String multiTenancy = jpaProperties.getProperties().get(AvailableSettings.MULTI_TENANT);
		String multi_tenant_connection_provider = jpaProperties.getProperties().get(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER);
    	if(StringUtils.isNotBlank(multiTenancy) && DataSourceMultiTenantConnectionProviderImpl.class.getName().equals(multi_tenant_connection_provider)){
    		vendorProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER,dataSourceMultiTenantConnectionProvider);
    	}
		return vendorProperties;
	}
	
	protected String[] getPackagesToScan() {
		if (AutoConfigurationPackages.has(this.beanFactory)) {
			List<String> basePackages = AutoConfigurationPackages.get(this.beanFactory);
			return basePackages.toArray(new String[basePackages.size()]);
		}
		return NO_PACKAGES;
	}
	
	protected void customizeVendorProperties(Map<String, Object> vendorProperties) {
		if (!vendorProperties.containsKey(JTA_PLATFORM)) {
			configureJtaPlatform(vendorProperties);
		}
	}
	
	private void configureJtaPlatform(Map<String, Object> vendorProperties)
			throws LinkageError {
		JtaTransactionManager jtaTransactionManager = getJtaTransactionManager();
		if (jtaTransactionManager != null) {
			if (runningOnWebSphere()) {
				// We can never use SpringJtaPlatform on WebSphere as
				// WebSphereUowTransactionManager has a null TransactionManager
				// which will cause Hibernate to NPE
				configureWebSphereTransactionPlatform(vendorProperties);
			}
			else {
				configureSpringJtaPlatform(vendorProperties, jtaTransactionManager);
			}
		}
		else {
			vendorProperties.put(JTA_PLATFORM, getNoJtaPlatformManager());
		}
	}
	
	private Object getJtaPlatformManager(String[] candidates) {
		for (String candidate : candidates) {
			try {
				return Class.forName(candidate).newInstance();
			}
			catch (Exception ex) {
				// Continue searching
			}
		}
		throw new IllegalStateException("Could not configure JTA platform");
	}
	
	private Object getNoJtaPlatformManager() {
		return getJtaPlatformManager(NO_JTA_PLATFORM_CLASSES);
	}
	
	private void configureSpringJtaPlatform(Map<String, Object> vendorProperties,
			JtaTransactionManager jtaTransactionManager) {
		try {
			vendorProperties.put(JTA_PLATFORM,
					new SpringJtaPlatform(jtaTransactionManager));
		}
		catch (LinkageError ex) {
			// NoClassDefFoundError can happen if Hibernate 4.2 is used and some
			// containers (e.g. JBoss EAP 6) wraps it in the superclass LinkageError
			if (!isUsingJndi()) {
				throw new IllegalStateException("Unable to set Hibernate JTA "
						+ "platform, are you using the correct "
						+ "version of Hibernate?", ex);
			}
			// Assume that Hibernate will use JNDI
			if (logger.isDebugEnabled()) {
				logger.debug("Unable to set Hibernate JTA platform : " + ex.getMessage());
			}
		}
	}
	
	private boolean isUsingJndi() {
		try {
			return JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable();
		}
		catch (Error ex) {
			return false;
		}
	}
	
	protected JtaTransactionManager getJtaTransactionManager() {
		return this.jtaTransactionManager;
	}
	
	private boolean runningOnWebSphere() {
		return ClassUtils.isPresent(
				"com.ibm.websphere.jtaextensions." + "ExtendedJTATransaction",
				getClass().getClassLoader());
	}
	
	private void configureWebSphereTransactionPlatform(
			Map<String, Object> vendorProperties) {
		vendorProperties.put(JTA_PLATFORM, getWebSphereJtaPlatformManager());
	}
	
	private Object getWebSphereJtaPlatformManager() {
		return getJtaPlatformManager(WEBSPHERE_JTA_PLATFORM_CLASSES);
	}
	
	protected final boolean isJta() {
		return (this.jtaTransactionManager != null);
	}
}
