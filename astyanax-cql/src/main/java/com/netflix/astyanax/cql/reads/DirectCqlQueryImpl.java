package com.netflix.astyanax.cql.reads;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Query;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.SimpleStatement;
import com.google.common.util.concurrent.ListenableFuture;
import com.netflix.astyanax.CassandraOperationType;
import com.netflix.astyanax.Serializer;
import com.netflix.astyanax.connectionpool.OperationResult;
import com.netflix.astyanax.connectionpool.exceptions.ConnectionException;
import com.netflix.astyanax.cql.CqlAbstractExecutionImpl;
import com.netflix.astyanax.cql.CqlFamilyFactory;
import com.netflix.astyanax.cql.CqlKeyspaceImpl.KeyspaceContext;
import com.netflix.astyanax.cql.writes.CqlColumnFamilyMutationImpl.ColumnFamilyMutationContext;
import com.netflix.astyanax.model.CqlResult;
import com.netflix.astyanax.query.CqlQuery;
import com.netflix.astyanax.query.PreparedCqlQuery;

public class DirectCqlQueryImpl<K, C> implements CqlQuery<K, C> {

	private final KeyspaceContext ksContext;
	private final ColumnFamilyMutationContext cfContext;
	private final String basicCqlQuery; 
	
	public DirectCqlQueryImpl(KeyspaceContext ksCtx, ColumnFamilyMutationContext cfCtx, String basicCqlQuery) {
		this.ksContext = ksCtx;
		this.cfContext = cfCtx;
		this.basicCqlQuery = basicCqlQuery;
	}
	
	@Override
	public OperationResult<CqlResult<K, C>> execute() throws ConnectionException {
		return new InternalExecutionImpl(new SimpleStatement(basicCqlQuery)).execute();
	}

	@Override
	public ListenableFuture<OperationResult<CqlResult<K, C>>> executeAsync() throws ConnectionException {
		return new InternalExecutionImpl(new SimpleStatement(basicCqlQuery)).executeAsync();
	}
	
	@Override
	public CqlQuery<K, C> useCompression() {
		throw new NotImplementedException();
	}

	@Override
	public PreparedCqlQuery<K, C> asPreparedStatement() {
		
		final BoundStatement boundStatement = new BoundStatement(ksContext.getSession().prepare(basicCqlQuery));
		final List<Object> bindList = new ArrayList<Object>();
		
		return new PreparedCqlQuery<K, C>() {

			@Override
			public OperationResult<CqlResult<K, C>> execute() throws ConnectionException {
				boundStatement.bind(bindList.toArray());
				return new InternalExecutionImpl(boundStatement).execute();
			}

			@Override
			public ListenableFuture<OperationResult<CqlResult<K, C>>> executeAsync() throws ConnectionException {
				
				boundStatement.bind(bindList.toArray());
				return new InternalExecutionImpl(boundStatement).executeAsync();
			}

			@Override
			public <V> PreparedCqlQuery<K, C> withByteBufferValue(V value, Serializer<V> serializer) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withValue(ByteBuffer value) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withValues(List<ByteBuffer> value) {
				bindList.addAll(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withStringValue(String value) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withIntegerValue(Integer value) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withBooleanValue(Boolean value) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withDoubleValue(Double value) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withLongValue(Long value) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withFloatValue(Float value) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withShortValue(Short value) {
				bindList.add(value);
				return this;
			}

			@Override
			public PreparedCqlQuery<K, C> withUUIDValue(UUID value) {
				bindList.add(value);
				return this;
			}
		};
	}
	
	private class InternalExecutionImpl extends CqlAbstractExecutionImpl<CqlResult<K, C>> {

		private final Query query;
		
		public InternalExecutionImpl(Query query) {
			super(ksContext, cfContext);
			this.query = query;
		}

		@Override
		public CassandraOperationType getOperationType() {
			return CassandraOperationType.CQL;
		}

		@Override
		public Query getQuery() {
			return query;
		}

		@Override
		public CqlResult<K, C> parseResultSet(ResultSet resultSet) {
			boolean isCountQuery = basicCqlQuery.contains(" count(");
			if (isCountQuery) {
				return new DirectCqlResult<K,C>(new Long(resultSet.one().getLong(0)));
			} else {
				boolean isOldStyle = CqlFamilyFactory.OldStyleThriftMode();
				return new DirectCqlResult<K,C>(resultSet.all(), cf, isOldStyle);
			}
		}
	}
}