/* ***** BEGIN LICENSE BLOCK *****
 * 
 * Copyright (c) 1997-2008 Aliasource - Groupe LINAGORA
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
 *  published by the Free Software Foundation; either version 2 of the
 *  License, (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  General Public License for more details.
 * 
 *  http://www.obm.org/                                              
 * 
 * ***** END LICENSE BLOCK ***** */
package org.obm.sync.server.handler;

import java.util.Date;
import java.util.Map;

import org.obm.sync.auth.AccessToken;
import org.obm.sync.auth.ServerFault;
import org.obm.sync.server.ParametersSource;
import org.obm.sync.server.XmlResponder;
import org.obm.sync.setting.ForwardingSettings;
import org.obm.sync.setting.VacationSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import fr.aliacom.obm.common.session.SessionManagement;
import fr.aliacom.obm.common.setting.SettingBindingImpl;

/**
 * Handles the following urls :
 * 
 * <code>/setting/getSettings?sid=xxx</code>
 */
@Singleton
public class SettingHandler extends SecureSyncHandler {

	private SettingBindingImpl binding;

	@Inject
	private SettingHandler(SessionManagement sessionManagement, SettingBindingImpl settingBindingImpl) {
		super(sessionManagement);
		this.binding = settingBindingImpl;
	}

	@Override
	public void handle(String method, ParametersSource params, XmlResponder responder)
		throws Exception {
		AccessToken at = getCheckedToken(params);
		if ("getSettings".equals(method)) {
			getSettings(at, responder);
		} else if ("setVacationSettings".equals(method)) {
			setVacationSettings(at, params, responder);
		} else if ("getVacationSettings".equals(method)) {
			getVacationSettings(at, responder);
		} else if ("setEmailForwarding".equals(method)) {
			setEmailForwarding(at, params, responder);
		} else if ("getEmailForwarding".equals(method)) {
			getEmailForwarding(at, responder);
		} else {
			responder.sendError("Cannot handle method '" + method + "'");
		}

	}

	private void getEmailForwarding(AccessToken at, XmlResponder responder) throws ServerFault {
		ForwardingSettings fs = binding.getEmailForwarding(at);
		responder.sendEmailForwarding(fs);

	}

	private void setEmailForwarding(AccessToken at, ParametersSource params,
			XmlResponder responder) throws ServerFault {
		ForwardingSettings fs = new ForwardingSettings();
		fs.setEnabled(Boolean.valueOf(params.getParameter("enabled")));
		fs.setLocalCopy(Boolean.valueOf(params.getParameter("localCopy")));
		fs.setEmail(params.getParameter("email"));
		binding.setEmailForwarding(at, fs);
		responder.sendString("Forwarding settings saved");
	}

	private void getVacationSettings(AccessToken at, XmlResponder responder) 
		throws ServerFault {
		VacationSettings vs = binding.getVacationSettings(at);
		responder.sendVacation(vs);
	}

	private void setVacationSettings(AccessToken at, ParametersSource params,
			XmlResponder responder) throws ServerFault {
		VacationSettings vs = new VacationSettings();
		vs.setEnabled(Boolean.valueOf(params.getParameter("enabled")));
		if (vs.isEnabled()) {
			long l;
			Date d;

			String s = params.getParameter("start");
			if (s != null) {
				l = Long.parseLong(s);
				d = new Date(l);
				vs.setStart(d);
			}

			s = params.getParameter("end");
			if (s != null) {
				l = Long.parseLong(s);
				d = new Date(l);
				vs.setEnd(d);
			}

			vs.setText(params.getParameter("text"));
		}
		binding.setVacationSettings(at, vs);
		responder.sendString("Vacation settings stored");
	}

	private void getSettings(AccessToken at, XmlResponder responder) throws ServerFault {
		Map<String, String> ret = binding.getSettings(at);
		responder.sendSettings(ret);
	}

}
