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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import junit.framework.TestCase;

import com.moss.error_reporting.api.ErrorReport;
import com.moss.error_reporting.api.ErrorReportChunk;
import com.moss.error_reporting.api.ReportId;

public class NotifierTestWorkbench extends TestCase {
	public static void main(String[] args) throws IOException {
		List<ErrorReportChunk> chunks = new LinkedList<ErrorReportChunk>();
		chunks.add(new ErrorReportChunk("a test", "text/plain", "Hello World".getBytes()));
		
		
		
		chunks.add(new ErrorReportChunk("log", "application/zip", toBytes(NotifierTestWorkbench.class.getResourceAsStream("/test.zip"))));
		ErrorReport report = new ErrorReport(chunks);
		Notifier n = new Notifier("mail.bellsouth.net", "stu@penrose.us", Arrays.asList(new String[]{
				"stu@mosscomputing.com",
				"stu@penrose.us"
		}));
		n.sendEmailNotification(new ReportId(UUID.randomUUID()), report);
		//		n.sendPlainEmail(report);
	}
	
	private static byte[] toBytes(InputStream in) throws IOException {
		ByteArrayOutputStream out = new  ByteArrayOutputStream();
		byte[] buffer = new byte[1024*100];
		for(int numRead = in.read(buffer);numRead!=-1;numRead =in.read(buffer)){
			out.write(buffer, 0, numRead);
		}
		out.close();
		in.close();
		return out.toByteArray();
	}
}
