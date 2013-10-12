/**
 * Copyright (C) 2013, Moss Computing Inc.
 *
 * This file is part of error-reporting.
 *
 * error-reporting is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * error-reporting is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with error-reporting; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package com.moss.error_reporting.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.SimpleEmail;

import com.moss.error_reporting.api.ErrorReport;
import com.moss.error_reporting.api.ErrorReportChunk;
import com.moss.error_reporting.api.ReportId;

public final class Notifier {

	private static int MAX_MESSAGE_LENGTH=1024*5;//5k

	private final Log log = LogFactory.getLog(getClass());
	private final List<String> emailRecipients = new LinkedList<String>();
	private final String emailFromAddress;
	private final String smtpServer;

	public Notifier(String smtpServer, String emailFromAddress, List<String> emailRecipients) {
		super();
		this.smtpServer = smtpServer;
		this.emailFromAddress = emailFromAddress;
		this.emailRecipients.addAll(emailRecipients);
	}

	public void sendNotifications(final ReportId id, final  ErrorReport report){
		new Thread(){
			@Override
			public void run() {
				if(smtpServer!=null && emailFromAddress !=null && emailRecipients!=null){
					log.info("Sending Email");
					sendEmailNotification(id, report);
					log.info("Email Sent");
				}
			}
		}.start();
	}

	public void sendPlainEmail(ReportId id, ErrorReport report){
		try {
			SimpleEmail email=new SimpleEmail();
			prepEmail(email, id);
			email.setMsg("hello world");
			email.send();
		} catch (EmailException e) {
			e.printStackTrace();
		}

	}

	private void prepEmail(Email email, ReportId id) throws EmailException {
		email.setHostName(smtpServer);
		for (String recipient : emailRecipients) {
			email.addTo(recipient);
		}

		//		email.addTo(emailRecipients.get(0));
		email.setFrom(emailFromAddress, "Error Report Server");
		email.setSubject("Error Report Received: " + id.getUuid());
	}

	public void sendEmailNotification(ReportId id, ErrorReport report){
		try {
			HtmlEmail email=new HtmlEmail();
			prepEmail(email, id);

			StringBuffer body = new StringBuffer();
			body.append("<html><body>");

			List<ErrorReportChunk> chunks = report.getReportChunks();
			for(int x=0;x<chunks.size();x++){

				ErrorReportChunk chunk = chunks.get(x);
				body.append("<div style=\"padding:5px;text-align:center;border:1px solid black;\">");
				body.append("<span style=\"font-weight:bold;\">" + chunk.getName() + "</span>");
				body.append("<span style=\"font-style:italic;\">[" + chunk.getMimeType() + "</span>]");
				body.append("</div>");
				String mimeType = chunk.getMimeType();
				if(mimeType!=null && mimeType.equals("text/plain")){
					body.append("<div style=\"border:1px solid black;border-top:0px;padding:5px;background:#dddddd;\"><pre>");
					boolean needsTruncation = chunk.getData().length>MAX_MESSAGE_LENGTH;
					int length = needsTruncation?MAX_MESSAGE_LENGTH:chunk.getData().length;

					body.append(new String(chunk.getData(), 0, length));
					if(needsTruncation){
						body.append("\n");
						body.append((chunk.getData().length-MAX_MESSAGE_LENGTH) + " more bytes");
					}
					body.append("</pre></div>");
				}else if(mimeType!=null && mimeType.equals("application/zip")){
					body.append("<div style=\"border:1px solid black;border-top:0px;padding:5px;background:#dddddd;\"><pre>");
					ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(chunk.getData()));
					try {
						new ZipToTextTool().run(in, body);
					} catch (IOException e) {
						e.printStackTrace();
						body.append("Error Reading Zip:" + e.getMessage());
					}
					body.append("</pre></div>");
				}
				if(x!=chunks.size()-1 && chunks.size()>1)
					body.append("<br/><br/>");

			}
			body.append("</body></html>");

			//			email.setHtmlMsg("<html><body>Hello World</body></html>");
			email.setHtmlMsg(body.toString());
			//			email.setTextMsg("HTML EMAIL");
			email.buildMimeMessage();
			email.sendMimeMessage();

		} catch (EmailException e) {
			e.printStackTrace();
		}
	}
}
