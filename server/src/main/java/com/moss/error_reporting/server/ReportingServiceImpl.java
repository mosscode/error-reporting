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

import java.io.File;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.jws.WebService;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.Instant;

import com.moss.error_reporting.api.ErrorReport;
import com.moss.error_reporting.api.ErrorReportSummary;
import com.moss.error_reporting.api.ReportId;
import com.moss.error_reporting.api.ReportingService;
import com.moss.error_reporting.server.store.DataStore;
import com.moss.jaxbhelper.JAXBHelper;

@WebService(
	endpointInterface=ReportingService.CLASS_NAME,
	serviceName=ReportingService.SERVICE_NAME,
	targetNamespace=ReportingService.NS
)
public final class ReportingServiceImpl implements ReportingService {
	private final Log log = LogFactory.getLog(getClass());
	
	private final DocumentRepository documentRepository;
	private final JAXBHelper helper;
	private final JAXBContext jaxb;
	private final Notifier notifier;
	
	public ReportingServiceImpl(DataStore data, Notifier notifier) {
		this.documentRepository = new DocumentRepository(data.getStorageDir());
		this.notifier = notifier;
		
		try {
			jaxb = JAXBContext.newInstance(ErrorReport.class);
			helper = new JAXBHelper(jaxb);
			
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	public ReportId submitReport(ErrorReport report) {
		System.out.println("Error report received.");
		
		Instant now = new Instant();
		ReportId id = new ReportId(UUID.randomUUID());
		report.setWhenReceived(now);
		
		try {
			
			String xmlString = helper.writeToXmlString(report);
			documentRepository.put(id.getUuid(), xmlString.getBytes());
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}

		notifier.sendNotifications(id, report);
		return id;
	}
	
	
	public ErrorReport getReport(ReportId id) {
		log.debug("Retrieving report " + id);
		InputStream stream = documentRepository.getStream(id.getUuid());
		
		log.debug("Stream is " + stream);
		
		if(stream == null) {
			return null;
		}

		ErrorReport report = null;
		try {
			log.debug("creating unmarshaller");
			Unmarshaller unmarshaller = jaxb.createUnmarshaller();
			log.debug("created it, now using it");
			report = (ErrorReport)unmarshaller.unmarshal(stream);
			stream.close();
			log.debug("report unmarshalled");
		} catch(Exception ex) {
			ex.printStackTrace();
		}
		
		return report;
	}
	
	public List<ErrorReportSummary> listReportSummaries() {
		List<ErrorReportSummary> summaries = new LinkedList<ErrorReportSummary>();
		
		String[] namesList = documentRepository.getDocumentsDirLocation().list();
		for(String name : namesList) {
			try {
				ReportId id = new ReportId(name);
				log.debug("version 0.0.3");
				log.debug("found report with id: "+name);
//				ErrorReport report = getReport(new ReportId(name));
				
				File file = documentRepository.path(id.getUuid());
				
				Instant lastModified = new Instant(file.lastModified());
				
				summaries.add(new ErrorReportSummary(id, lastModified));
				
			} catch(Exception ex) {
				log.info(ex.getStackTrace());
			}
		}
			
		return summaries;
	}
	
}
