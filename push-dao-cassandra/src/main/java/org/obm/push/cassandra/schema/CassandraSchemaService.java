/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2014 Linagora
 *
 * This program is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU Affero General Public License as 
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version, provided you comply 
 * with the Additional Terms applicable for OBM connector by Linagora 
 * pursuant to Section 7 of the GNU Affero General Public License, 
 * subsections (b), (c), and (e), pursuant to which you must notably (i) retain 
 * the “Message sent thanks to OBM, Free Communication by Linagora” 
 * signature notice appended to any and all outbound messages 
 * (notably e-mail and meeting requests), (ii) retain all hypertext links between 
 * OBM and obm.org, as well as between Linagora and linagora.com, and (iii) refrain 
 * from infringing Linagora intellectual property rights over its trademarks 
 * and commercial brands. Other Additional Terms apply, 
 * see <http://www.linagora.com/licenses/> for more details. 
 *
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details. 
 *
 * You should have received a copy of the GNU Affero General Public License 
 * and its applicable Additional Terms for OBM along with this program. If not, 
 * see <http://www.gnu.org/licenses/> for the GNU Affero General Public License version 3 
 * and <http://www.linagora.com/licenses/> for the Additional Terms applicable to 
 * OBM connectors. 
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm.push.cassandra.schema;

import static org.obm.push.cassandra.OpushCassandraModule.LATEST_SCHEMA_VERSION_NAME;
import static org.obm.push.cassandra.OpushCassandraModule.MINIMAL_SCHEMA_VERSION_NAME;
import static org.obm.push.cassandra.schema.StatusSummary.Status.NOT_INITIALIZED;
import static org.obm.push.cassandra.schema.StatusSummary.Status.UPGRADE_AVAILABLE;
import static org.obm.push.cassandra.schema.StatusSummary.Status.UPGRADE_REQUIRED;
import static org.obm.push.cassandra.schema.StatusSummary.Status.UP_TO_DATE;

import org.obm.breakdownduration.bean.Watch;
import org.obm.push.bean.BreakdownGroups;
import org.obm.push.cassandra.dao.CassandraSchemaDao;
import org.obm.push.cassandra.dao.SchemaProducer;
import org.obm.push.cassandra.exception.BadVersionException;
import org.obm.push.cassandra.exception.InstallSchemaNotFoundException;
import org.obm.push.cassandra.exception.NoTableException;
import org.obm.push.cassandra.exception.NoVersionException;
import org.obm.push.cassandra.exception.SchemaOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Session;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
@Watch(BreakdownGroups.CASSANDRA)
public class CassandraSchemaService {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(CassandraSchemaService.class);

	private final CassandraSchemaDao schemaDao;
	private final SchemaProducer schemaProducer;
	private final Session session;
	private final Version minimalVersionUpdate;
	private final Version latestVersionUpdate;

	@Inject
	@VisibleForTesting CassandraSchemaService(
			CassandraSchemaDao schemaDao,
			SchemaProducer schemaProducer,
			Session session,
			@Named(MINIMAL_SCHEMA_VERSION_NAME) Version minimalVersionUpdate,
			@Named(LATEST_SCHEMA_VERSION_NAME) Version latestVersionUpdate) {
		
		this.schemaDao = schemaDao;
		this.schemaProducer = schemaProducer;
		this.session = session;
		this.minimalVersionUpdate = minimalVersionUpdate;
		this.latestVersionUpdate = latestVersionUpdate;
	}
	
	public StatusSummary getStatus() {
		try {
			VersionUpdate currentVersion = schemaDao.getCurrentVersion();
			
			if (currentVersion.getVersion().isGreaterThanOrEqual(latestVersionUpdate)) {
				return StatusSummary.status(UP_TO_DATE).currentVersion(currentVersion).build();
			} else if (currentVersion.getVersion().isLessThan(minimalVersionUpdate)) {
				return StatusSummary.status(UPGRADE_REQUIRED).currentVersion(currentVersion).upgradeAvailable(latestVersionUpdate).build();
			} else {
				return StatusSummary.status(UPGRADE_AVAILABLE).currentVersion(currentVersion).upgradeAvailable(latestVersionUpdate).build();
			}
		} catch (NoTableException e) {
			return StatusSummary.status(NOT_INITIALIZED).upgradeAvailable(latestVersionUpdate).build();
		} catch (NoVersionException e) {
			return StatusSummary.status(UPGRADE_REQUIRED).upgradeAvailable(latestVersionUpdate).build();
		}
	}

	public CQLScriptExecutionStatus install() throws SchemaOperationException {
		try {
			String schema = schemaProducer.schema(latestVersionUpdate);
			if (Strings.isNullOrEmpty(schema)) {
				throw new InstallSchemaNotFoundException();
			}
			
			executeCQL(schema);
			
			schemaDao.updateVersion(latestVersionUpdate);
			return CQLScriptExecutionStatus.OK;
		} catch (Exception e) {
			throw new SchemaOperationException(e);
		}
	}

	private void executeCQL(String cql) {
		LOGGER.debug("Execute Cassandra CQL: {}", cql);
		for (String subQuery : subQueries(cql)) {
			session.execute(subQuery);
		}
	}

	@VisibleForTesting Iterable<String> subQueries(String schema) {
		Iterable<String> subQueries = Splitter.on(";").trimResults().split(schema);
		return Iterables.filter(subQueries, new Predicate<String>() {

			@Override
			public boolean apply(String query) {
				if (Strings.isNullOrEmpty(query) || System.lineSeparator().equals(query)) {
					return false;
				}
				return true;
			}
		});
	}
	
	public CQLScriptExecutionStatus update() throws SchemaOperationException {
		try {
			Version currentVersion = schemaDao.getCurrentVersion().getVersion();
			if (currentVersion.equals(latestVersionUpdate)) {
				return CQLScriptExecutionStatus.OK;
			}
			if (latestVersionUpdate.isLessThan(currentVersion)) {
				throw new BadVersionException(currentVersion, latestVersionUpdate); 
			}
			
			executeCQL(schemaProducer.schema(currentVersion, latestVersionUpdate));
			
			schemaDao.updateVersion(latestVersionUpdate);
			return CQLScriptExecutionStatus.OK;
		} catch (BadVersionException e) {
			throw e;
		} catch (Exception e) {
			throw new SchemaOperationException(e);
		}
	}
}
