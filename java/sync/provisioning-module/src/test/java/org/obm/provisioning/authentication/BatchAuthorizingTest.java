/* ***** BEGIN LICENSE BLOCK *****
 *
 * Copyright (C) 2011-2013  Linagora
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
package org.obm.provisioning.authentication;

import static com.jayway.restassured.RestAssured.given;
import static org.easymock.EasyMock.expect;

import javax.ws.rs.core.Response.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.obm.filter.Slow;
import org.obm.guice.GuiceModule;
import org.obm.guice.SlowGuiceRunner;
import org.obm.provisioning.CommonDomainEndPointEnvTest;
import org.obm.provisioning.beans.Batch;
import org.obm.provisioning.beans.BatchStatus;

import com.google.common.collect.ImmutableSet;

@Slow
@RunWith(SlowGuiceRunner.class)
@GuiceModule(CommonDomainEndPointEnvTest.Env.class)
public class BatchAuthorizingTest extends CommonDomainEndPointEnvTest {
	
	@Test
	public void testSubjectCannotAuthenticateWithNoDomain() {
		mocksControl.replay();
		
		given()
			.auth().basic("username", "password").
		expect()
			.statusCode(Status.UNAUTHORIZED.getStatusCode()).
		when()
			.get("/batches/1");
		
		mocksControl.verify();
	}
	
	@Test
	public void testSubjectCannotGetWithWrongPassword() {
		expectAuthenticatingReturns("username", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "wrongPassword").
		expect()
			.statusCode(Status.UNAUTHORIZED.getStatusCode()).
		when()
			.get("/batches/1");
		
		mocksControl.verify();
	}
	
	@Test
	public void testSubjectCannotPostWithWrongPassword() {
		expectAuthenticatingReturns("username", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "wrongPassword").
		expect()
			.statusCode(Status.UNAUTHORIZED.getStatusCode()).
		when()
			.post("/batches");
		
		mocksControl.verify();
	}
	
	@Test
	public void testSubjectCannotDeleteWithWrongPassword() {
		expectAuthenticatingReturns("username", "password");
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "wrongPassword").
		expect()
			.statusCode(Status.UNAUTHORIZED.getStatusCode()).
		when()
			.delete("/batches/1");
		
		mocksControl.verify();
	}
	
	@Test
	public void testSubjectCannotGetWithWrongPermissions() throws Exception {
		expectAuthenticatingReturns("username", "password");
		expectAuthorizingReturns("username", ImmutableSet.of("batches:create, delete"));
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "password").
		expect()
			.statusCode(Status.UNAUTHORIZED.getStatusCode()).
		when()
			.get("/batches/1");
		
		mocksControl.verify();
	}
	
	@Test
	public void testSubjectCannotPostWithWrongPermissions() throws Exception {
		expectAuthenticatingReturns("username", "password");
		expectAuthorizingReturns("username", ImmutableSet.of("batches:read, delete"));
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "password").
		expect()
			.statusCode(Status.UNAUTHORIZED.getStatusCode()).
		when()
			.post("/batches");
		
		mocksControl.verify();
	}
	
	@Test
	public void testSubjectCannotDeleteWithWrongPermissions() throws Exception {
		expectAuthenticatingReturns("username", "password");
		expectAuthorizingReturns("username", ImmutableSet.of("batches:read, create"));
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "password").
		expect()
			.statusCode(Status.UNAUTHORIZED.getStatusCode()).
		when()
			.delete("/batches/1");
		
		mocksControl.verify();
	}

	@Test
	public void testSubjectCanGetBatchWithReadPermission() throws Exception {
		expectDomain();
		expectBatch();
		expectAuthenticatingReturns("username", "password");
		expectAuthorizingReturns("username", ImmutableSet.of("batches:read"));
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "password").
		expect()
			.statusCode(Status.OK.getStatusCode()).
		when()
			.get("/batches/1");
		
		mocksControl.verify();
	}
	
	@Test
	public void testSubjectCanPostBatchWithCreatePermission() throws Exception {
		Batch.Builder batchBuilder = Batch
				.builder()
				.status(BatchStatus.IDLE)
				.domain(domain);
		
		expectDomain();
		expect(batchDao.create(batchBuilder.build())).andReturn(batchBuilder.id(batchId(1)).build());
		expectAuthenticatingReturns("username", "password");
		expectAuthorizingReturns("username", ImmutableSet.of("batches:create"));
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "password").
		expect()
			.statusCode(Status.CREATED.getStatusCode()).
		when()
			.post("/batches");
		
		mocksControl.verify();
	}
	
	@Test
	public void testSubjectCanDeleteBatchWithDeletePermission() throws Exception {
		expectDomain();
		batchDao.delete(batchId(1));
		expectAuthenticatingReturns("username", "password");
		expectAuthorizingReturns("username", ImmutableSet.of("batches:delete"));
		mocksControl.replay();
		
		given()
			.auth().basic("username@domain", "password").
		expect()
			.statusCode(Status.OK.getStatusCode()).
		when()
			.delete("/batches/1");
		
		mocksControl.verify();
	}
}
