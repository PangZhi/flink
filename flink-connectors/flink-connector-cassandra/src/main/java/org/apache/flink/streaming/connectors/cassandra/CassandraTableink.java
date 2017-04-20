/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.streaming.connectors.cassandra;

import com.datastax.driver.core.Cluster;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.table.sinks.StreamTableSink;
import org.apache.flink.table.sinks.TableSink;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * A cassandra  {@link StreamTableSink}.
 *
 */
class CassandraTableink implements StreamTableSink<Row> {
	private final List<InetSocketAddress> hostAddrs;
	private final String cql;
	private final String[] fieldNames;
	private final TypeInformation[] fieldTypes;
	private final Properties properties;

	public CassandraTableink(List<InetSocketAddress> hostAddrs, String cql, String[] fieldNames, TypeInformation[] fieldTypes, Properties properties) {
		this.hostAddrs = Preconditions.checkNotNull(hostAddrs, "hostAddrs");
		this.cql = Preconditions.checkNotNull(cql, "cql");
		this.fieldNames = Preconditions.checkNotNull(fieldNames, "fieldNames");
		this.fieldTypes = Preconditions.checkNotNull(fieldTypes, "fieldTypes");
		this.properties = Preconditions.checkNotNull(properties, "properties");
	}

	@Override
	public TypeInformation<Row> getOutputType() {
		return new RowTypeInfo(fieldTypes);
	}

	@Override
	public String[] getFieldNames() {
		return this.fieldNames;
	}

	@Override
	public TypeInformation<?>[] getFieldTypes() {
		return this.fieldTypes;
	}

	@Override
	public TableSink<Row> configure(String[] fieldNames, TypeInformation<?>[] fieldTypes) {
		return new CassandraTableink(this.hostAddrs, this.cql, this.fieldNames, this.fieldTypes, this.properties);
	}

	@Override
	public void emitDataStream(DataStream<Row> dataStream) {
		try {
			CassandraSink.addSink(dataStream)
				.setClusterBuilder(new CassandraClusterBuilder(this.hostAddrs))
				.setQuery(this.cql)
				.build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private class CassandraClusterBuilder extends ClusterBuilder {
		private final Collection<InetSocketAddress> hostAddrs;

		CassandraClusterBuilder(Collection<InetSocketAddress> hostAddrs) {
			this.hostAddrs = hostAddrs;
		}

		@Override
		protected Cluster buildCluster(Cluster.Builder builder) {
			return builder.addContactPointsWithPorts(hostAddrs).build();
		}
	}
}
