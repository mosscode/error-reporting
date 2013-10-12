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
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpStatus;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

import com.moss.error_reporting.api.ErrorReport;
import com.moss.error_reporting.api.ErrorReportChunk;
import com.moss.error_reporting.server.store.DataStore;

public final class HtmlReportRenderingHandler extends AbstractHandler {
	
	private static int MAX_MESSAGE_LENGTH=1024*5;//5k
	
	private final JAXBContext ctx;
	private final DocumentRepository repo;
	private final Handler handler;
	
	public HtmlReportRenderingHandler(DataStore data, Handler handler) throws Exception {
		ctx = JAXBContext.newInstance(ErrorReport.class);
		repo = new DocumentRepository(data.getStorageDir());
		this.handler = handler;
	}

	public void handle(String target, HttpServletRequest request, HttpServletResponse response, int dispatch) throws IOException, ServletException {
		
		System.out.println("Target: " + target);
		
		if (!target.startsWith("/report")) {
			handler.handle(target, request, response, dispatch);
			return;
		}
		
		((Request)request).setHandled(true);
		
    	if ("GET".equals(request.getMethod())) {
    		
    		boolean fail = false;
    		
    		String reportIdString = request.getParameter("id");
    		
    		if (reportIdString == null) {
    			response.sendError(HttpStatus.ORDINAL_400_Bad_Request, "The parameter 'id' is required");
    			fail = true;
    		}
    		
    		UUID reportId = null;
    		if (!fail) {
    			try {
    				reportId = UUID.fromString(reportIdString);
    			}
    			catch (IllegalArgumentException ex) {
    				response.sendError(HttpStatus.ORDINAL_400_Bad_Request, "The parameter 'id' is invalid");
    				fail = true;
    			}
    		}
    		
    		if (!fail) {
    			
    			if (!repo.exists(reportId)) {
    				response.sendError(HttpStatus.ORDINAL_404_Not_Found, "Cannot find error report " + reportId);
    				fail = true;
    			}
    			else {
    				try {
    					InputStream stream = repo.getStream(reportId);
    					
    					Unmarshaller u = ctx.createUnmarshaller();
    					ErrorReport report = (ErrorReport)u.unmarshal(stream);
    					
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
    					
    					byte[] content = body.toString().getBytes();
    					
    					response.setContentType("text/html");
    					response.setContentLength(content.length);
    					response.setStatus(HttpStatus.ORDINAL_200_OK);
    					
    					response.getOutputStream().write(content);
    				}
    				catch (Exception ex) {
    					response.sendError(HttpStatus.ORDINAL_500_Internal_Server_Error);
        				fail = true;
    				}
    			}
    		}
    	}
    	else {
    		response.sendError(HttpStatus.ORDINAL_405_Method_Not_Allowed);
    	}
    	
    	response.getOutputStream().close();
	}

	protected void doStart() throws Exception {
		super.doStart();
		handler.start();
	}
}
