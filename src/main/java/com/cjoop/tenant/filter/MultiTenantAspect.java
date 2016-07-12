package com.cjoop.tenant.filter;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.ast.statement.SQLJoinTableSource;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.ast.statement.SQLSubqueryTableSource;
import com.alibaba.druid.sql.ast.statement.SQLTableSource;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUnionQuery;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;
import com.cjoop.tenant.config.MultiTenantProperties;
import com.cjoop.tenant.dao.MultiTenantInfoDao;
import com.cjoop.tenant.domain.TenantEntity;
import com.cjoop.tenant.util.MultiTenantUtil;

/**
 * 针对jpa持久层的多租户切面拦截实现,只拦截租户类型为DISCRIMINATOR的租户
 * @author 陈均
 */
@Aspect
@Component("multiTenantAspect")
public class MultiTenantAspect{
	private static final String TENANT_FILTER = "tenantFilter";
	protected Log logger = LogFactory.getLog(getClass());
	Pattern insertPattern = Pattern.compile("^insert.+\\(.+\\).+\\(.+\\)$");
	@Autowired
	private MultiTenantProperties multiTenantProperties;
	@Autowired
	protected MultiTenantInfoDao multiTenantInfoDao;
	/**
	 * 拦截hql的查询过滤
	 * @param joinPoint 连接点信息
	 * @param entityManager entityManager
	 */
	@AfterReturning(pointcut = "execution(* javax.persistence.EntityManagerFactory.createEntityManager(..))", returning = "entityManager")
	public void createEntityManager(JoinPoint joinPoint, EntityManager entityManager) {
			String tenantId = getTenantId();
			Session session = entityManager.unwrap(Session.class);
			if(MultiTenantUtil.DEFAULT_TENANT_ID.equals(tenantId)){
				session.disableFilter(TENANT_FILTER);
			}else{
				session.enableFilter(TENANT_FILTER).setParameter("tenantFilterParam",tenantId);
			}
	}
	
	/**
	 * 拦截hql的查询过滤
	 * @param joinPoint 连接点信息
	 * @param entityManager entityManager
	 */
	@AfterReturning(pointcut = "execution(* javax.persistence.EntityManager.createQuery(..))")
	public void createQuery(JoinPoint joinPoint) {
		String tenantId = getTenantId();
		EntityManager entityManager = (EntityManager) joinPoint.getTarget();
		Session session = null;
		try {
			session = entityManager.unwrap(Session.class);
		} catch (Exception e) {
			return;
		}
		if(MultiTenantUtil.DEFAULT_TENANT_ID.equals(tenantId)){
			session.disableFilter(TENANT_FILTER);
		}else{
			session.enableFilter(TENANT_FILTER).setParameter("tenantFilterParam",tenantId);
		}
	}
	
	/**
	 * 拦截EntityManager.persist
	 * @param joinPoint 连接点信息
	 * @throws Throwable 调用异常
	 */
	@Around("execution(* javax.persistence.EntityManager.persist(..))")
	public Object persist(ProceedingJoinPoint joinPoint) throws Throwable{
		return saveOrUpdate(joinPoint);
	}

	private Object saveOrUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[]args = joinPoint.getArgs();
		Object entity = args[0];
		if(entity instanceof TenantEntity){
			TenantEntity tenantEntity = (TenantEntity)entity;
			tenantEntity.setTenantId(getTenantId());
		}else{
			logger.error("实体类:" + entity.getClass() + "不符合租户类型");
		}
		return joinPoint.proceed(args);
	}
	
	/**
	 * 拦截EntityManager.merge
	 * @param joinPoint 连接点信息
	 * @throws Throwable 调用异常
	 */
	@Around("execution(* javax.persistence.EntityManager.merge(..))")
	public Object merge(ProceedingJoinPoint joinPoint) throws Throwable{
		return saveOrUpdate(joinPoint);
	}
	
	
	/**
	 * 拦截原生sql的查询
	 * @param joinPoint 连接点信息
	 * @throws Throwable 调用异常
	 */
	@Around("execution(* javax.persistence.EntityManager.createNativeQuery(..))")
	public Object createNativeQuery(ProceedingJoinPoint joinPoint) throws Throwable{
		Object[]args = joinPoint.getArgs();
		String tenantId = getTenantId();
		if(!MultiTenantUtil.DEFAULT_TENANT_ID.equals(tenantId)){
			//租户信息过滤
			String tenantFieldName = getTenantFieldName();
			String sqlString = args[0].toString().toLowerCase().trim();
			if(sqlString.startsWith("select")){//查询过滤
				args[0] = generateMultiTenancySQL(sqlString);
			}else if(sqlString.startsWith("insert")){//插入过滤
				Matcher matcher = insertPattern.matcher(sqlString);
				if(matcher.find()){
					String[]array = sqlString.split("values");
					args[0] = array[0].replaceFirst("\\)", ","+tenantFieldName+")") + "values" + array[1].replaceFirst("\\)", ",'"+tenantId+"')");
				}else{
					throw new Exception("该语句不支持多租户,请使用以下格式:INSERT INTO table_name (列1, 列2,...) VALUES (值1, 值2,....)");
				}
			}else if(sqlString.startsWith("update") || sqlString.startsWith("delete")){//更新或删除过滤
				if(sqlString.contains("where")){
					args[0] = sqlString.replace("where", "where "+tenantFieldName+"='"+tenantId+"' and ");
				}else{
					args[0] = sqlString + " where "+tenantFieldName+"='"+1+"'";
				}
			}
		}
		return joinPoint.proceed(args);
	}
	
	/**
	 * 获取租户id信息
	 * @return tenantId
	 */
	public String getTenantId(){
		return MultiTenantUtil.getTenantId();
		
	}
	
	/**
	 * 获取租户字段名信息
	 * @return tenantFieldName
	 */
	public String getTenantFieldName(){
		return multiTenantProperties.getFieldName();
	}
	
	public String generateMultiTenancySQL(String sql) {
		MySqlStatementParser parser = new MySqlStatementParser(sql.replace("?", "!:"));
		List<SQLStatement> statementList = parser.parseStatementList();
		SQLStatement stmt = statementList.get(0);
		SQLSelectStatement selectStmt = (SQLSelectStatement) stmt;
		SQLSelect select = selectStmt.getSelect();
		appendMultiTenancy(select.getQuery());
		return generateSQL(statementList).replace("!:", "?");
	}
	
	private void appendMultiTenancy(SQLSelectQuery query) {
		if (query instanceof MySqlSelectQueryBlock) {
			appendMultiTenancy((MySqlSelectQueryBlock) query);
		} else if (query instanceof MySqlUnionQuery) {
			appendMultiTenancy(((MySqlUnionQuery) query).getLeft());
			appendMultiTenancy(((MySqlUnionQuery) query).getRight());
		}

	}

	private void appendMultiTenancy(MySqlSelectQueryBlock queryBlock) {
		String name = null;
		StringBuilder sb = new StringBuilder();
		SQLTableSource tableSource = queryBlock.getFrom();
		if (tableSource instanceof SQLExprTableSource) {
			name = tableSource.getAlias() == null ? tableSource.toString() : tableSource.getAlias();
		} else if (tableSource instanceof SQLSubqueryTableSource) {
			MySqlSelectQueryBlock queryBlockT = (MySqlSelectQueryBlock) ((SQLSubqueryTableSource) tableSource)
					.getSelect().getQuery();
			appendMultiTenancy(queryBlockT);
			return;
		} else if (tableSource instanceof SQLJoinTableSource) {
			if (((SQLJoinTableSource) tableSource).getJoinType().name
					.equals(SQLJoinTableSource.JoinType.RIGHT_OUTER_JOIN.name)) {
				tableSource = ((SQLJoinTableSource) tableSource).getRight();
				name = tableSource.toString();
				if (tableSource instanceof SQLExprTableSource) {
					if (tableSource.getAlias() != null) {
						name = tableSource.getAlias();
					}
				} else if (tableSource instanceof SQLSubqueryTableSource) {
					appendMultiTenancy(
							(MySqlSelectQueryBlock) ((SQLSubqueryTableSource) tableSource).getSelect().getQuery());
					return;
				}
			} else {
				tableSource = ((SQLJoinTableSource) tableSource).getLeft();
				name = tableSource.toString();
				if (tableSource instanceof SQLExprTableSource) {
					if (tableSource.getAlias() != null) {
						name = tableSource.getAlias();
					}
				} else if (tableSource instanceof SQLSubqueryTableSource) {
					appendMultiTenancy(
							(MySqlSelectQueryBlock) ((SQLSubqueryTableSource) tableSource).getSelect().getQuery());
					return;
				}
			}

		}
		sb.append(name + ".");
		SQLExpr exprObj = SQLUtils.toSQLExpr(sb.append(getTenantFieldName()).append(" = '")
				.append(getTenantId()).append("'").toString());
		SQLExpr newCondition = SQLUtils.buildCondition(SQLBinaryOperator.BooleanAnd, exprObj, false,
				queryBlock.getWhere());
		queryBlock.setWhere(newCondition);
	}

	private String generateSQL(List<SQLStatement> stmtList) {
		StringBuilder out = new StringBuilder();
		MySqlOutputVisitor visitor = new MySqlOutputVisitor(out);
		visitor.setPrettyFormat(false);
		for (SQLStatement stmt : stmtList) {
			stmt.accept(visitor);
		}
		return out.toString();
	}

}
