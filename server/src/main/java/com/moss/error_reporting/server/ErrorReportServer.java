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

import javax.xml.bind.JAXBContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;

import com.moss.error_reporting.api.ReportingService;
import com.moss.error_reporting.server.store.DataStore;
import com.moss.jaxbhelper.JAXBHelper;
import com.moss.rpcutil.jetty.ContentHandler;
import com.moss.rpcutil.jetty.SwitchingContentHandler;
import com.moss.rpcutil.jetty.hessian.HessianContentHandler;
import com.moss.rpcutil.jetty.jaxws.JAXWSContentHandler;

public final class ErrorReportServer {
	public static void main(String[] args) throws Exception {
		
		File log4jConfigFile = new File("log4j.xml");
		
		if (log4jConfigFile.exists()) {
			DOMConfigurator.configureAndWatch(log4jConfigFile.getAbsolutePath(), 1000);
			System.out.println("Configuring with log file " + log4jConfigFile.getAbsolutePath());
		}
		else {
			BasicConfigurator.configure();
			Logger.getRootLogger().setLevel(Level.INFO);
		}
		
		ServerConfiguration config;
		
		JAXBContext context = JAXBContext.newInstance(ServerConfiguration.class);
		JAXBHelper helper = new JAXBHelper(context);
		
		File configFile = new File("error-report-server.xml");
		if(!configFile.exists())
			configFile = new File(new File(System.getProperty("user.dir")), ".error-report-server.xml");
		if(!configFile.exists())
			configFile = new File("/etc/error-report-server.xml");
		
		if(!configFile.exists()){
			
			config = new ServerConfiguration();
			helper.writeToFile(helper.writeToXmlString(config), configFile);
			System.out.println("Created default config file at " + configFile.getAbsolutePath());
			
		}else{
			System.out.println("Reading config file at " + configFile.getAbsolutePath());
			config = helper.readFromFile(configFile);
		}
		new ErrorReportServer(config);
	}

	public ErrorReportServer(ServerConfiguration config) {
		
		final Log log = LogFactory.getLog(this.getClass());
		
		try {
			Server jetty = new Server();
			SelectChannelConnector connector = new SelectChannelConnector();
			connector.setPort(config.bindPort());
			connector.setHost(config.bindAddress());
			jetty.addConnector(connector);
			
			boolean initialize = false;
			if(!config.storageDir().exists() || config.storageDir().listFiles().length==0){
				initialize = true;
			}
			
			log.info("Using storage dir: " + config.storageDir().getAbsolutePath());
			
			DataStore data = new DataStore(config.storageDir(), initialize);
			ReportingServiceImpl impl = new ReportingServiceImpl(data, new Notifier(config.smtpServer(), config.fromAddress(), config.emailReceipients()));
			
			final String path = "/Reporting";
			final SwitchingContentHandler handler = new SwitchingContentHandler(path);
			final boolean jaxwsEnabled = Boolean.parseBoolean(System.getProperty("shell.rpc.jaxws.enabled", "true"));
			
			if (jaxwsEnabled) {

				if (log.isDebugEnabled()) {
					log.debug("Constructing JAXWS http content handler RPC impl " + impl.getClass().getSimpleName());
				}

				try {
					ContentHandler jaxwsHandler = new JAXWSContentHandler(impl);
					handler.addHandler(jaxwsHandler);
				}
				catch (Exception ex) {
					throw new RuntimeException("Failed to create JAXWS service for " + impl.getClass().getName(), ex);
				}
			}
		
			if (log.isDebugEnabled()) {
				log.debug("Constructing Hessian http content handler for RPC impl " + impl.getClass().getSimpleName());
			}

			ContentHandler hessianHandler = new HessianContentHandler(ReportingService.class, impl);
			handler.addHandler(hessianHandler);
		
			jetty.addHandler(handler);
			jetty.start();
			
			System.out.println("\n\nSERVER READY FOR ACTION\n\n");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unexpected Error.  Shutting Down.");
			System.exit(1);
		}
	}
}
