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
package org.obm.push.mail.bean;

import java.io.Serializable;

import org.obm.push.bean.DeviceId;
import org.obm.push.bean.SyncKey;
import org.obm.push.bean.User;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class WindowingKey implements Serializable {
	
	private static final long serialVersionUID = 8820449234854614206L;
	
	private final User user;
	private final DeviceId deviceId;
	private final int collectionId;
	private final SyncKey syncKey;

	
	public WindowingKey(User user, DeviceId deviceId, int collectionId, SyncKey syncKey) {
		Preconditions.checkArgument(user != null);
		Preconditions.checkArgument(deviceId != null);
		Preconditions.checkArgument(collectionId > 0);
		Preconditions.checkArgument(syncKey != null);
		this.user = user;
		this.deviceId = deviceId;
		this.collectionId = collectionId;
		this.syncKey = syncKey;
	}
	
	public User getUser() {
		return user;
	}

	public DeviceId getDeviceId() {
		return deviceId;
	}

	public int getCollectionId() {
		return collectionId;
	}

	public SyncKey getSyncKey() {
		return syncKey;
	}

	@Override
	public final int hashCode(){
		return Objects.hashCode(user, deviceId, collectionId, syncKey);
	}
	
	@Override
	public final boolean equals(Object object){
		if (object instanceof WindowingKey) {
			WindowingKey that = (WindowingKey) object;
			return Objects.equal(this.user, that.user)
				&& Objects.equal(this.deviceId, that.deviceId)
				&& Objects.equal(this.collectionId, that.collectionId)
				&& Objects.equal(this.syncKey, that.syncKey);
		}
		return false;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
			.add("user", user)
			.add("deviceId", deviceId)
			.add("collectionId", collectionId)
			.add("syncKey", syncKey)
			.toString();
	}
}