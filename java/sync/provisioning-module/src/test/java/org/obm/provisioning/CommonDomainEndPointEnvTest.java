/* ***** BEGIN LICENSE BLOCK *****
 *
 * Copyright (C) 2011-2012  Linagora
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
package org.obm.provisioning;

import static org.easymock.EasyMock.createControl;
import static org.easymock.EasyMock.expect;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.http.entity.StringEntity;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.easymock.IMocksControl;
import org.junit.After;
import org.junit.Before;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.obm.DateUtils;
import org.obm.configuration.DatabaseConfiguration;
import org.obm.cyrus.imap.admin.CyrusImapService;
import org.obm.dbcp.DatabaseConfigurationFixtureH2;
import org.obm.dbcp.DatabaseConnectionProvider;
import org.obm.domain.dao.DomainDao;
import org.obm.domain.dao.UserDao;
import org.obm.domain.dao.UserSystemDao;
import org.obm.provisioning.beans.Batch;
import org.obm.provisioning.beans.BatchEntityType;
import org.obm.provisioning.beans.BatchStatus;
import org.obm.provisioning.beans.HttpVerb;
import org.obm.provisioning.beans.Operation;
import org.obm.provisioning.beans.ProfileEntry;
import org.obm.provisioning.dao.BatchDao;
import org.obm.provisioning.dao.PermissionDao;
import org.obm.provisioning.dao.ProfileDao;
import org.obm.provisioning.dao.exceptions.DaoException;
import org.obm.provisioning.dao.exceptions.ProfileNotFoundException;
import org.obm.provisioning.ldap.client.Configuration;
import org.obm.provisioning.ldap.client.LdapService;
import org.obm.provisioning.ldap.client.StaticConfiguration;
import org.obm.provisioning.processing.BatchProcessor;
import org.obm.provisioning.processing.BatchTracker;
import org.obm.push.utils.UUIDFactory;
import org.obm.satellite.client.SatelliteService;
import org.obm.sync.date.DateProvider;
import org.obm.sync.host.ObmHost;
import org.obm.sync.serviceproperty.ServiceProperty;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.util.Modules;
import com.jayway.restassured.RestAssured;
import com.sun.jersey.guice.JerseyServletModule;

import fr.aliacom.obm.common.domain.ObmDomain;
import fr.aliacom.obm.common.domain.ObmDomainUuid;
import fr.aliacom.obm.common.user.ObmUser;
import fr.aliacom.obm.common.user.UserExtId;

public abstract class CommonDomainEndPointEnvTest {

	public static class Env extends AbstractModule {

		private Server server;
		private Context context;
		
		@Override
		protected void configure() {
			
			server = createServer();
			bind(Server.class).toInstance(server);
			
			context = createContext(server);
			
			install(Modules.override(new ProvisioningService(context.getServletContext())).with(new JerseyServletModule() {

				private IMocksControl mocksControl = createControl();

				@Override
				protected void configureServlets() {
					bind(IMocksControl.class).toInstance(mocksControl);
					bind(UserDao.class).toInstance(mocksControl.createMock(UserDao.class));
					bind(DomainDao.class).toInstance(mocksControl.createMock(DomainDao.class));
					bind(BatchDao.class).toInstance(mocksControl.createMock(BatchDao.class));
					bind(UserSystemDao.class).toInstance(mocksControl.createMock(UserSystemDao.class));
					bind(ProfileDao.class).toInstance(mocksControl.createMock(ProfileDao.class));
					bind(PermissionDao.class).toInstance(mocksControl.createMock(PermissionDao.class));
					bind(ResourceForTest.class);
					bind(SatelliteService.class).toInstance(mocksControl.createMock(SatelliteService.class));
					bind(BatchProcessor.class).toInstance(mocksControl.createMock(BatchProcessor.class));
					bind(DomainBasedSubResourceForTest.class);
					bind(CyrusImapService.class).toInstance(mocksControl.createMock(CyrusImapService.class));

					bind(DateProvider.class).toInstance(mocksControl.createMock(DateProvider.class));
					bind(DatabaseConnectionProvider.class).toInstance(mocksControl.createMock(DatabaseConnectionProvider.class));
					bind(DatabaseConfiguration.class).to(DatabaseConfigurationFixtureH2.class);
					bind(Configuration.class).to(StaticConfiguration.class);
					bind(LdapService.class).toInstance(mocksControl.createMock(LdapService.class));
					bind(BatchTracker.class).toInstance(mocksControl.createMock(BatchTracker.class));
				}
			}));
		}
		
		private Context createContext(Server server) {
			Context root = new Context(server, "/", Context.SESSIONS);

			root.addFilter(GuiceFilter.class, "/*", 0);
			root.addServlet(DefaultServlet.class, "/*");
			
			return root;
		}
		
		private Server createServer() {
			Server server = new Server(0);
			return server;
		}
	}
	
	protected static final ProfileName adminProfile = ProfileName.builder().name("admin").build();

	protected static final ProfileName userProfile = ProfileName.builder().name("user").build();

	protected static final ObmDomain domain = ObmDomain
			.builder()
			.name("domain")
			.id(1)
			.uuid(ObmDomainUuid.of("a3443822-bb58-4585-af72-543a287f7c0e"))
			.host(
					ServiceProperty.IMAP,
					ObmHost.builder().name("host").build())
			.host(ServiceProperty.LDAP, ObmHost.builder().ip("1.2.3.4").build())
			.build();
	
	protected static final ProfileName profileName = ProfileName
			.builder()
			.name("profile")
			.build();
	
	protected static final ProfileEntry profileEntry = ProfileEntry
			.builder()
			.domainUuid(domain.getUuid())
			.id(1)
			.build();
	
	protected static final Batch batch = Batch
			.builder()
			.id(batchId(1))
			.domain(domain)
			.status(BatchStatus.ERROR)
			.operation(Operation
					.builder()
					.id(operationId(1))
					.status(BatchStatus.SUCCESS)
					.entityType(BatchEntityType.USER)
					.request(org.obm.provisioning.beans.Request
							.builder()
							.resourcePath("/users")
							.verb(HttpVerb.POST)
							.body("{\"id\":123456}")
							.build())
					.build())
			.operation(Operation
					.builder()
					.id(operationId(2))
					.status(BatchStatus.ERROR)
					.entityType(BatchEntityType.USER)
					.error("Invalid User")
					.request(org.obm.provisioning.beans.Request
							.builder()
							.resourcePath("/users/1")
							.verb(HttpVerb.PATCH)
							.body("{}")
							.build())
					.build())
			.build();

	@Inject
	protected IMocksControl mocksControl;
	@Inject
	protected Server server;
	@Inject
	protected DomainDao domainDao;
	@Inject
	protected UserDao userDao;
	@Inject
	protected BatchDao batchDao;
	@Inject
	protected ProfileDao profileDao;
	@Inject
	protected PermissionDao roleDao;
	@Inject
	protected Realm realm;

	protected String baseUrl;
	protected int serverPort;
	@Inject
	private UUIDFactory uuidFactory;

	@Before
	public void setUp() throws Exception {
		server.start();
		serverPort = server.getConnectors()[0].getLocalPort();
		baseUrl = "http://localhost:" + serverPort + ProvisioningService.PROVISIONING_URL_PREFIX;

		SecurityUtils.setSecurityManager(new DefaultWebSecurityManager(realm));
		RestAssured.baseURI = baseUrl + "/" + domain.getUuid().get();
		RestAssured.port = serverPort;
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
	}

	protected String domainAwarePerm(String permission) {
		return String.format("%s:%s", domain.getUuid().get(), permission);
	}

	protected void expectDomain() {
		expect(domainDao.findDomainByUuid(domain.getUuid())).andReturn(domain).atLeastOnce();
	}

	protected void expectBatch() throws DaoException {
		expect(batchDao.get(batch.getId())).andReturn(batch);
	}
	
	protected void expectProfiles() throws DaoException {
		expect(profileDao.getProfiles(domain.getUuid())).andReturn(
				ImmutableSet.of(profileEntry, profileEntry));
	}
	
	protected void expectProfile() throws DaoException, ProfileNotFoundException {
		expect(profileDao.getProfile(domain.getUuid(),
				ProfileId.builder().id(profileEntry.getId()).build())).andReturn(profileName);
	}

	protected void expectNoDomain() {
		expect(domainDao.findDomainByUuid(domain.getUuid())).andReturn(null);
	}

	protected void expectNoBatch() throws DaoException {
		expect(batchDao.get(batch.getId())).andReturn(null);
	}
	
	protected void expectSuccessfulAuthentication(String login, String password) {
		ObmUser user = ObmUser.builder()
						.login(login)
						.password(password)
						.domain(domain)
						.lastName(login)
						.firstName(login)
						.extId(UserExtId
								.builder()
								.extId(uuidFactory.randomUUID().toString())
								.build())
						.build();
		expect(domainDao.findDomainByName(domain.getName())).andReturn(domain);
		expect(userDao.findUserByLogin(login, domain)).andReturn(user);
	}
	
	protected void expectAuthorizingReturns(String login, Collection<String> permissions) throws Exception {
		expect(profileDao.getProfileForUser(login, domain.getUuid())).andReturn(adminProfile);
		expect(domainDao.findDomainByName(domain.getName())).andReturn(domain);
		expect(roleDao.getPermissionsForProfile(adminProfile, domain)).andReturn(permissions);
	}
	
	protected void expectSuccessfulAuthenticationAndFullAuthorization() throws Exception {
		expectSuccessfulAuthentication("username", "password");
		expectAuthorizingReturns("username", ImmutableSet.of("*"));
	}
	
	public static Batch.Id batchId(Integer id) {
		return Batch.Id.builder().id(id).build();
	}

	public static Operation.Id operationId(Integer id) {
		return Operation.Id.builder().id(id).build();
	}

	protected Operation operation(BatchEntityType entityType, String path, String entity, HttpVerb verb, Map<String, String> params) {
		return Operation
				.builder()
				.entityType(entityType)
				.status(BatchStatus.IDLE)
				.request(org.obm.provisioning.beans.Request
						.builder()
						.resourcePath(domain.getUuid().get() + path)
						.body(entity)
						.verb(verb)
						.params(params)
						.build())
				.build();
	}

	protected StringEntity obmUserToJson() throws UnsupportedEncodingException {
		final StringEntity userToJson = new StringEntity(obmUserToJsonString());
		userToJson.setContentType(MediaType.APPLICATION_JSON);
		return  userToJson;
	}

	protected String obmUserToJsonString() {
		return 	
			"{" +
				"\"id\":\"extId\"," +
				"\"login\":\"user1\"," +
				"\"lastname\":\"Doe\"," +
				"\"profile\":\"Utilisateurs\"," +
				"\"firstname\":\"Jesus\"," +
				"\"commonname\":\"John Doe\"," +
				"\"password\":\"password\"," +
				"\"kind\":\"kind\"," +
				"\"title\":\"title\"," +
				"\"description\":\"description\"," +
				"\"company\":\"company\"," +
				"\"service\":\"service\"," +
				"\"direction\":\"direction\"," +
				"\"addresses\":[\"address1\",\"address2\"]," +
				"\"town\":\"town\"," +
				"\"zipcode\":\"zipcode\"," +
				"\"business_zipcode\":\"1234\"," +
				"\"country\":\"1234\"," +
				"\"phones\":[\"phone\",\"phone2\"]," +
				"\"mobile\":\"mobile\"," +
				"\"faxes\":[\"fax\",\"fax2\"]," +
				"\"mail_quota\":\"1234\"," +
				"\"mail_server\":\"host\"," +
				"\"mails\":[\"john@domain\"]," +
				"\"timecreate\":\"2013-06-11T12:00:00.000+0000\"," +
				"\"timeupdate\":\"2013-06-11T13:00:00.000+0000\"," +
				"\"groups\":[\"Not implemented yet\"]" +
			"}";
	}
	
	protected ObmUser fakeUser() {
		return ObmUser.builder()
				.domain(domain)
				.extId(userExtId("extId"))
				.login("user1")
				.password("password")
				.lastName("Doe")
				.profileName(ProfileName.valueOf("Utilisateurs"))	// Not implemented yet in ObmUser
				.firstName("Jesus")
				.commonName("John Doe")
				.kind("kind")					// Not implemented yet in ObmUser
				.title("title")
				.description("description")
				.company("company")				// Not implemented yet in ObmUser
				.service("service")
				.direction("direction")				// Not implemented yet in ObmUser
				.address1("address1")
				.address2("address2")
				.town("town")
				.zipCode("zipcode")
				.expresspostal("1234")
				.countryCode("1234")
				.phone("phone")
				.phone2("phone2")
				.mobile("mobile")
				.fax("fax")
				.fax2("fax2")
				.mailQuota(1234)
				.mailHost(ObmHost.builder().name("host").build())			// Not implemented yet in ObmUser
				.emailAndAliases("john@domain")
				.timeCreate(DateUtils.date("2013-06-11T14:00:00"))
				.timeUpdate(DateUtils.date("2013-06-11T15:00:00"))
				//.groups()					// Not implemented yet in ObmUser
				.build();
	}

	protected UserExtId userExtId(String extId) {
		return UserExtId.builder().extId(extId).build();
	}
}
