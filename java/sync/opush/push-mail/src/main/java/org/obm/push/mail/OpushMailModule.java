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
package org.obm.push.mail;

import org.obm.configuration.EmailConfiguration;
import org.obm.configuration.EmailConfigurationImpl;
import org.obm.push.backend.MailMonitoringBackend;
import org.obm.push.backend.PIMBackend;
import org.obm.push.mail.imap.ImapClientProvider;
import org.obm.push.mail.imap.ImapClientProviderImpl;
import org.obm.push.mail.imap.ImapMailboxService;
import org.obm.push.mail.imap.ImapMonitoringImpl;
import org.obm.push.mail.imap.ImapStoreManager;
import org.obm.push.mail.imap.ImapStoreManagerImpl;
import org.obm.push.mail.imap.MessageInputStreamProvider;
import org.obm.push.mail.imap.MessageInputStreamProviderImpl;
import org.obm.push.mail.smtp.SmtpProvider;
import org.obm.push.mail.smtp.SmtpProviderImpl;
import org.obm.push.mail.transformer.HtmlToText;
import org.obm.push.mail.transformer.Identity;
import org.obm.push.mail.transformer.Transformer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

public class OpushMailModule extends AbstractModule {

	@Override
	protected void configure() {
		bind(MailMonitoringBackend.class).to(ImapMonitoringImpl.class);
		bind(MailboxService.class).to(ImapMailboxService.class);
		bind(MailBackend.class).to(MailBackendImpl.class);
		bind(ImapStoreManager.class).to(ImapStoreManagerImpl.class);
		bind(MessageInputStreamProvider.class).to(MessageInputStreamProviderImpl.class);
		bind(ImapClientProvider.class).to(ImapClientProviderImpl.class);
		bind(EmailConfiguration.class).to(EmailConfigurationImpl.class);
		bind(SmtpProvider.class).to(SmtpProviderImpl.class);
		Multibinder<PIMBackend> pimBackends = 
				Multibinder.newSetBinder(binder(), PIMBackend.class);
		pimBackends.addBinding().to(MailBackend.class);
		bind(MailViewToMSEmailConverter.class).to(MailViewToMSEmailConverterImpl.class);
		Multibinder<Transformer.Factory> transformers = 
				Multibinder.newSetBinder(binder(), Transformer.Factory.class);
		transformers.addBinding().to(Identity.Factory.class);
		transformers.addBinding().to(HtmlToText.Factory.class);
	}

}
