/**
 * Copyright (c) 2011 Optimax Software Ltd
 * 
 * This file is part of ElasticInbox.
 * 
 * ElasticInbox is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * ElasticInbox is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * ElasticInbox. If not, see <http://www.gnu.org/licenses/>.
 */

package com.elasticinbox.core.message;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMessage.RecipientType;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.ParseException;
import javax.mail.util.SharedByteArrayInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elasticinbox.common.utils.Assert;
import com.elasticinbox.core.model.Address;
import com.elasticinbox.core.model.AddressList;
import com.elasticinbox.core.model.Message;
import com.elasticinbox.core.model.MimePart;
import com.sun.mail.util.BASE64DecoderStream;
import com.sun.mail.util.QPDecoderStream;

public final class MimeParser
{
	public final static String MIME_HEADER_SPAM = "X-Spam-Flag";
	private static Properties props = new Properties();

	private Message message;
	private MimeMessage mimeMessage;

    private StringBuilder textBody = new StringBuilder();
    private StringBuilder htmlBody = new StringBuilder();

	private static final Logger logger = LoggerFactory
			.getLogger(MimeParser.class);

	static {
		// Make JavaMail parser more error tolerant
		// see http://javamail.kenai.com/nonav/javadocs/javax/mail/internet/package-summary.html#package_description
		props.setProperty("mail.mime.address.strict", "false");
		props.setProperty("mail.mime.decodetext.strict", "false");
		props.setProperty("mail.mime.decodefilename", "true");
		props.setProperty("mail.mime.decodeparameters", "true");
		props.setProperty("mail.mime.charset", "utf-8");
		props.setProperty("mail.mime.parameters.strict", "false");
		props.setProperty("mail.mime.base64.ignoreerrors", "true");
		props.setProperty("mail.mime.uudecode. ignoreerrors", "true");
		props.setProperty("mail.mime.uudecode.ignoremissingbeginend", "true");
		props.setProperty("mail.mime.multipart.allowempty", "true");
		props.setProperty("mail.mime.ignoreunknownencoding", "true");
		props.setProperty("mail.mime.ignoremultipartencoding", "false");
		props.setProperty("mail.mime.allowencodedmessages", "true");

		// Some of JavaMail properties should be set on System level 
		for (Iterator<Object> iter = props.keySet().iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			System.setProperty(key, (String) props.get(key));
		}
	}

	public MimeParser() {
		//
	}

	public MimeParser(InputStream in) throws IOException, MimeParserException {
		parse(in);
	}

	/**
	 * Parse {@link InputStream} into {@link Message} structure
	 * 
	 * @param in
	 * @throws IOException
	 * @throws MimeParserException
	 */
	public void parse(InputStream in) throws IOException, MimeParserException
	{
		this.message = new Message();
		Session session = Session.getDefaultInstance(props);

		try {
			this.mimeMessage = new MimeMessage(session, in);
			this.message.setFrom(getAddressList(mimeMessage.getFrom()));
			this.message.setTo(getAddressList(mimeMessage.getRecipients(RecipientType.TO)));
			this.message.setCc(getAddressList(mimeMessage.getRecipients(RecipientType.CC)));
			this.message.setBcc(getAddressList(mimeMessage.getRecipients(RecipientType.BCC)));
			this.message.setSubject(mimeMessage.getSubject());
			this.message.setMessageId(mimeMessage.getMessageID());
			this.message.setDate(mimeMessage.getSentDate());
			//this.message.setSize((long) mimeMessage.getSize());

			// extract necessary minor headers
			// TODO: This should be replaced by filters in future
			message.addMinorHeader(MIME_HEADER_SPAM, mimeMessage.getHeader(MIME_HEADER_SPAM, null));

			// extract mime parts and body
			parseMessagePart(mimeMessage, "");
		} catch (MessagingException e) {
			logger.error("Unable to parse MIME message: ", e);
			throw new MimeParserException(e.getMessage());
		}

		if (this.htmlBody.length() > 0) {
			message.setHtmlBody(this.htmlBody.toString());
		}

		if (this.textBody.length() > 0) {
			message.setPlainBody(this.textBody.toString());
		}
	}

	public Message getMessage() throws IOException {
		return message;
	}

	/**
	 * Get InputStream of MIME part identified by Part ID
	 * 
	 * @param contentId
	 * @return
	 * @throws MimeParserException
	 */
	public InputStream getInputStreamByPartId(String partId)
			throws MimeParserException
	{
		Assert.notNull(this.mimeMessage, "No message was processed. Initialize first.");
		message.getPart(partId); // make sure that part exists, otherwise IAE will be thrown

		MimeMultipart mp;
		Object content;
		InputStream in = null;

		// find based on Part ID eg. 1.2.3
		try {
			mp = (MimeMultipart) this.mimeMessage.getContent();
			String[] partNums = partId.split("\\.");

			// loop through parts to reach the final part
			for (int i = 0; i < partNums.length; i++)
			{
				int localPartId = Integer.parseInt(partNums[i]) - 1;
				content = mp.getBodyPart(localPartId).getContent();

				if (content instanceof MimeMultipart) {
					mp = (MimeMultipart) content;
				} else if ((content instanceof String)
						|| (content instanceof BASE64DecoderStream)
						|| (content instanceof QPDecoderStream)
						|| (content instanceof MimeMessage)
						|| (content instanceof ByteArrayInputStream)
						|| (content instanceof SharedByteArrayInputStream)) {
					in = mp.getBodyPart(localPartId).getInputStream();
				} else {
					// normally, we should never get here
					// perhaps bad Part ID
					throw new MessagingException("MIME part not found");
				}
			}
		} catch (IOException e) {
			throw new MimeParserException("Unable to extract attachment from the message: " + e.getMessage());
		} catch (MessagingException e) {
			throw new IllegalArgumentException("Message does not contain part with ID " + partId);
		}

		return in;
	}

	/**
	 * Get InputStream of MIME part identified by Content-ID
	 * 
	 * @param contentId
	 * @return
	 * @throws MimeParserException
	 */
	public InputStream getInputStreamByContentId(String contentId)
			throws MimeParserException
	{
		Assert.notNull(this.mimeMessage, "No message was processed. Initialize first.");
		message.getPartByContentId(contentId); // make sure that part exists, otherwise IAE will be thrown

		MimeMultipart mp;
		InputStream in = null;

		try {
			mp = (MimeMultipart) this.mimeMessage.getContent();
			in = mp.getBodyPart("<"+contentId+">").getInputStream();
		} catch (IOException e) {
			throw new MimeParserException("Unable to extract attachment from the message: " + e.getMessage());
		} catch (MessagingException e) {
			throw new IllegalArgumentException("Message does not contain part with Content-ID " + contentId);
		}

		return in;
	}

	/**
	 * Recursively walk through parsed MIME message and extract parts info
	 * 
	 * @throws IOException
	 * @throws MessagingException
	 */
	private void parseMessagePart(Part part, String partId) throws IOException,
			MessagingException
	{
		Object content = null;

		// decode part
		try {
			content = part.getContent();
		} catch (UnsupportedEncodingException uee) {
			// TODO: make better handling of unsupported encodings, perhaps using jcharset detector
			if (part.isMimeType("text/*")) {
				// decode text as ISO-8859-1 for all unknown encodings
				logger.error("Parser detected unsupported encoding: {}, will try decoding with ISO-8859-1", uee.getMessage());
				InputStream in = part.getInputStream();
				content = IOUtils.toString(in, "ISO-8859-1");
			} else {
				logger.error("Parser detected unsupported encoding: {}. Don't know how to decode.", uee.getMessage());
				textBody.append("Unknown Encoding. Unable to display message contents.");
				return;
			}
		}

		logger.debug("Parsing part {} with mime type {}.",
				(partId.isEmpty()) ? "message" : partId, part.getContentType());

		if (content instanceof String) {
			// simple part with text
			
			String dis = null;

			try {
				dis = part.getDisposition();
			} catch (ParseException e) {
				// if parsing of disposition string failed, assume part an attachment
				dis = Part.ATTACHMENT; 
			}

			logger.trace("MIME parser extracted TEXT part: {}", (String) content);

			if ((dis != null) && dis.equals(Part.ATTACHMENT)) {
				// add text part as attachment
				message.addPart(partId, new MimePart(part));
			} else {
				// if no disposition, then add to message body
				if(part.isMimeType("text/html")) {
					htmlBody.append((String) content);
				} else {
					textBody.append((String) content);
				}
			}
		} else if (content instanceof MimeMultipart) {
			MimeMultipart multipart = (MimeMultipart) content;

			for (int i = 0; i <  multipart.getCount(); i++)
			{
				// build next part id
				StringBuilder nextPartId = new StringBuilder(partId);

				// add period if not at root level
				if (!partId.isEmpty())
					nextPartId.append(".");

				int localPartId = i+1; // IMAPv4 MIME part counter starts from 1
				nextPartId.append(localPartId);

				Part nextPart = multipart.getBodyPart(i);
				parseMessagePart(nextPart, nextPartId.toString());
			}
		} else if ((content instanceof BASE64DecoderStream)
				|| (content instanceof QPDecoderStream)
				|| (content instanceof MimeMessage)
				|| (content instanceof ByteArrayInputStream)
				|| (content instanceof SharedByteArrayInputStream)) {
			// binary, message/rfc822 or text attachment
			message.addPart(partId, new MimePart(part));
		} else {
			throw new MessagingException("Unkonwn message part type " + content.getClass().getName());
		}
	}

	/**
	 * Get AddressList from JavaMail Address array
	 * 
	 * @param mailboxes MailboxList
	 * @return AddressList
	 * @throws IllegalArgumentException
	 */
	private static AddressList getAddressList(javax.mail.Address[] al)
			throws IllegalArgumentException
	{
		if (al == null)
			return null;

		ArrayList<Address> addresses = new ArrayList<Address>();
		
		for (int i = 0; i < al.length; i++) {
			InternetAddress ia = (InternetAddress) al[i];
			Address a = new Address(ia.getPersonal(), ia.getAddress());
			addresses.add(a);
		}

		return new AddressList(addresses);
	}

}
