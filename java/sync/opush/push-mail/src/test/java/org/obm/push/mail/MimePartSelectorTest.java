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

import java.util.List;

import org.easymock.EasyMock;
import org.fest.assertions.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.minig.imap.mime.ContentType;
import org.minig.imap.mime.MimeMessage;
import org.minig.imap.mime.MimePart;
import org.obm.push.bean.BodyPreference;
import org.obm.push.bean.MSEmailBodyType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;


public class MimePartSelectorTest {

	private MimePartSelector mimeMessageSelector;

	@Before
	public void init() {
		mimeMessageSelector = new MimePartSelector();
	}
	
	@Test
	public void testSelectPlainText() {
		MimePart expectedMimePart = new MimePart();
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/plain"))).andReturn(expectedMimePart);
		
		EasyMock.replay(mimeMessage);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(
				Lists.newArrayList(bodyPreference(MSEmailBodyType.PlainText)), mimeMessage);
		EasyMock.verify(mimeMessage);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
	}
	
	@Test
	public void testSelectHtml() {
		MimePart expectedMimePart = new MimePart();
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/html"))).andReturn(expectedMimePart);
		
		EasyMock.replay(mimeMessage);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(
				Lists.newArrayList(bodyPreference(MSEmailBodyType.HTML)), mimeMessage);
		EasyMock.verify(mimeMessage);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
	}
	
	@Test
	public void testSelectRtf() {
		MimePart expectedMimePart = new MimePart();
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/rtf"))).andReturn(expectedMimePart);
		
		EasyMock.replay(mimeMessage);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(
				Lists.newArrayList(bodyPreference(MSEmailBodyType.RTF)), mimeMessage);
		EasyMock.verify(mimeMessage);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
	}
	
	@Test
	public void testSelectMime() {
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		
		EasyMock.replay(mimeMessage);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(
				Lists.newArrayList(bodyPreference(MSEmailBodyType.MIME)), mimeMessage);
		EasyMock.verify(mimeMessage);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(mimeMessage);
	}

	@Test
	public void testSelectDefaultBodyPreferences() {
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/plain"))).andReturn(null);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/html"))).andReturn(null);
		
		EasyMock.replay(mimeMessage);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(ImmutableList.<BodyPreference>of(), mimeMessage);
		EasyMock.verify(mimeMessage);
		
		Assertions.assertThat(mimePartSelector).isNull();
	}
	
	@Test
	public void testSelectSeveralBodyPreferences() {
		MimePart expectedMimePart = new MimePart();
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/rtf"))).andReturn(null);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/html"))).andReturn(expectedMimePart);
		
		EasyMock.replay(mimeMessage);
		List<BodyPreference> bodyPreferences = 
				Lists.newArrayList(
						bodyPreference(MSEmailBodyType.RTF), bodyPreference(MSEmailBodyType.HTML));
		FetchInstructions mimePartSelector = mimeMessageSelector.select(bodyPreferences, mimeMessage);
		EasyMock.verify(mimeMessage);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
	}
	
	@Test
	public void testSelectSeveralBodyPreferencesReturnMimeMessage() {
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/rtf"))).andReturn(null);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/html"))).andReturn(null);
		
		EasyMock.replay(mimeMessage);
		List<BodyPreference> bodyPreferences = 
				Lists.newArrayList(
						bodyPreference(MSEmailBodyType.RTF), 
						bodyPreference(MSEmailBodyType.HTML), 
						bodyPreference(MSEmailBodyType.MIME));
		FetchInstructions mimePartSelector = mimeMessageSelector.select(bodyPreferences, mimeMessage);
		EasyMock.verify(mimeMessage);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(mimeMessage);
	}
	
	@Test
	public void testSelectLargerThanQueryPreferencesWithAllOrNone() {
		MimePart expectedMimePart = EasyMock.createStrictMock(MimePart.class);
		EasyMock.expect(expectedMimePart.getSize()).andReturn(50);
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/plain"))).andReturn(expectedMimePart);
		
		BodyPreference bodyPreference = new BodyPreference.Builder().
				bodyType(MSEmailBodyType.PlainText).truncationSize(10).allOrNone(true).build();
		
		EasyMock.replay(mimeMessage, expectedMimePart);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(Lists.newArrayList(bodyPreference), mimeMessage);
		EasyMock.verify(mimeMessage, expectedMimePart);
		
		Assertions.assertThat(mimePartSelector).isNull();
	}
	
	@Test
	public void testSelectSmallerThanQueryPreferencesWithAllOrNone() {
		MimePart expectedMimePart = EasyMock.createStrictMock(MimePart.class);
		EasyMock.expect(expectedMimePart.getSize()).andReturn(10);
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/plain"))).andReturn(expectedMimePart);
		
		BodyPreference bodyPreference = new BodyPreference.Builder().
				bodyType(MSEmailBodyType.PlainText).truncationSize(50).allOrNone(true).build();
		
		EasyMock.replay(mimeMessage, expectedMimePart);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(Lists.newArrayList(bodyPreference), mimeMessage);
		EasyMock.verify(mimeMessage, expectedMimePart);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
		Assertions.assertThat(mimePartSelector.getTruncation()).isEqualTo(50);
	}
	
	@Test
	public void testSelectAllOrNoneWithoutTruncationSize() {
		MimePart expectedMimePart = EasyMock.createStrictMock(MimePart.class);
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/plain"))).andReturn(expectedMimePart);
		
		BodyPreference bodyPreference = new BodyPreference.Builder().
				bodyType(MSEmailBodyType.PlainText).allOrNone(true).build();
		
		EasyMock.replay(mimeMessage, expectedMimePart);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(Lists.newArrayList(bodyPreference), mimeMessage);
		EasyMock.verify(mimeMessage, expectedMimePart);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
		Assertions.assertThat(mimePartSelector.getTruncation()).isNull();
	}
	
	@Test
	public void testSelectWithoutAllOrNoneAndTruncationSize() {
		MimePart expectedMimePart = EasyMock.createStrictMock(MimePart.class);
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/plain"))).andReturn(expectedMimePart);
		
		BodyPreference bodyPreference = new BodyPreference.Builder().
				bodyType(MSEmailBodyType.PlainText).allOrNone(false).build();
		
		EasyMock.replay(mimeMessage, expectedMimePart);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(Lists.newArrayList(bodyPreference), mimeMessage);
		EasyMock.verify(mimeMessage, expectedMimePart);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
		Assertions.assertThat(mimePartSelector.getTruncation()).isNull();
	}
	
	@Test
	public void testSelectTruncationWithoutAllOrNone() {
		MimePart expectedMimePart = EasyMock.createStrictMock(MimePart.class);
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/plain"))).andReturn(expectedMimePart);
		
		BodyPreference bodyPreference = new BodyPreference.Builder().
				bodyType(MSEmailBodyType.PlainText).truncationSize(10).allOrNone(false).build();
		
		EasyMock.replay(mimeMessage, expectedMimePart);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(Lists.newArrayList(bodyPreference), mimeMessage);
		EasyMock.verify(mimeMessage, expectedMimePart);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
		Assertions.assertThat(mimePartSelector.getTruncation()).isEqualTo(10);
	}
	
	@Test
	public void testSelectTruncatedMimePartSeveralBodyPreferences() {
		MimePart plainTextMimePart = EasyMock.createStrictMock(MimePart.class);
		EasyMock.expect(plainTextMimePart.getSize()).andReturn(50);
		
		MimePart expectedMimePart = EasyMock.createStrictMock(MimePart.class);
		EasyMock.expect(expectedMimePart.getSize()).andReturn(10);
		
		MimeMessage mimeMessage = EasyMock.createStrictMock(MimeMessage.class);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/rtf"))).andReturn(null);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/plain"))).andReturn(plainTextMimePart);
		EasyMock.expect(mimeMessage.findMainMessage(contentType("text/html"))).andReturn(expectedMimePart);
		
		BodyPreference rtfBodyPreference = new BodyPreference.Builder().bodyType(MSEmailBodyType.RTF).build();
		BodyPreference plainTextBodyPreference = new BodyPreference.Builder().
				bodyType(MSEmailBodyType.PlainText).truncationSize(10).allOrNone(true).build();
		
		BodyPreference htmlBodyPreference = new BodyPreference.Builder().
				bodyType(MSEmailBodyType.HTML).truncationSize(50).allOrNone(true).build();
		
		List<BodyPreference> bodyPreferences = Lists.newArrayList(
				rtfBodyPreference, 
				plainTextBodyPreference, 
				htmlBodyPreference);

		EasyMock.replay(mimeMessage, plainTextMimePart, expectedMimePart);
		FetchInstructions mimePartSelector = mimeMessageSelector.select(bodyPreferences, mimeMessage);
		EasyMock.verify(mimeMessage, plainTextMimePart, expectedMimePart);
		
		Assertions.assertThat(mimePartSelector.getMimePart()).isNotNull().isSameAs(expectedMimePart);
		Assertions.assertThat(mimePartSelector.getTruncation()).isEqualTo(50);
	}
	
	private ContentType contentType(String mimeType) {
		return new ContentType.Builder().contentType(mimeType).build();
	}
	
	private BodyPreference bodyPreference(MSEmailBodyType emailBodyType) {
		 return new BodyPreference.Builder().bodyType(emailBodyType).build();
	}
}