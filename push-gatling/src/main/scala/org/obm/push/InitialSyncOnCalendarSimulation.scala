/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (C) 2011-2014  Linagora
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
package org.obm.push

import org.obm.push.bean.FolderType
import org.obm.push.command.FolderSyncCommand
import org.obm.push.command.InitialFolderSyncContext
import org.obm.push.command.InitialSyncContext
import org.obm.push.command.ProvisioningCommand
import org.obm.push.command.SyncCollectionCommand
import org.obm.push.context.Configuration
import org.obm.push.context.GatlingConfiguration
import org.obm.push.context.UserKey
import org.obm.push.wbxml.WBXMLTools

import io.gatling.core.Predef.Simulation
import io.gatling.core.Predef.atOnce
import io.gatling.core.Predef.{scenario => createScenario}
import io.gatling.core.Predef.userNumber
import io.gatling.http.Predef.http
import io.gatling.http.Predef.httpProtocolBuilder2HttpProtocol
import io.gatling.http.Predef.requestBuilder2ActionBuilder

object InitialSyncOnCalendarScenarioBuilder extends ScenarioBuilder {

	val wbTools: WBXMLTools = new WBXMLTools
	val userKey = new UserKey("user")

	override def build(configuration: Configuration) = 
		createScenario("Initial Sync on user's calendar")
			.exec(ProvisioningCommand.buildInitialProvisioningCommand(userKey))
			.exec(ProvisioningCommand.buildAcceptProvisioningCommand(userKey))
			.exec(buildInitialFolderSyncCommand(userKey))
			.exec(buildSyncOnCalendarCommand(userKey))
	
	def buildSyncOnCalendarCommand(userKey: UserKey) = {
		val initialSyncContext = new InitialSyncContext(userKey, FolderType.DEFAULT_CALENDAR_FOLDER)
		new SyncCollectionCommand(initialSyncContext, wbTools).buildCommand
	}
	
	def buildInitialFolderSyncCommand(userKey: UserKey) = {
		new FolderSyncCommand(new InitialFolderSyncContext(userKey), wbTools).buildCommand
	}
}
