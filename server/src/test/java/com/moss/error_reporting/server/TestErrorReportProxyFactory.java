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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.moss.error_reporting.api.ReportingService;
import com.moss.rpcutil.proxy.ProxyFactory;
import com.moss.rpcutil.proxy.ProxyProvider;
import com.moss.rpcutil.proxy.hessian.HessianProxyProvider;
import com.moss.rpcutil.proxy.jaxws.JAXWSProxyProvider;

public final class TestErrorReportProxyFactory {
	
	public static final String PROXY_PROVIDER_PROPERTY = "proxyProvider";
	public static final ProxyProviderOption DEFAULT_OPTION = ProxyProviderOption.JAXWS;
	
	public static enum ProxyProviderOption {
		JAXWS() {
			public ProxyProvider makeProvider() {
				JAXWSProxyProvider prov = new JAXWSProxyProvider()
				.register(ReportingService.class, ReportingService.QNAME);
				return prov;
			}
		},
		HESSIAN() {
			public ProxyProvider makeProvider() {
				return new HessianProxyProvider();
			}
		};
		
		abstract ProxyProvider makeProvider();
	}
	
	public static ProxyFactory create() {
		return create(null, DEFAULT_OPTION);
	}
	
	public static ProxyFactory create(ProxyProviderOption option) {
		return create(option, DEFAULT_OPTION);
	}
	
	private static ProxyFactory create(ProxyProviderOption option, ProxyProviderOption defaultOption) {
		
		Log log = LogFactory.getLog(TestErrorReportProxyFactory.class);
		
		if (option == null) {
			
			if (log.isDebugEnabled()) {
				log.debug("No option was provided as an argument, attempting to determine option from system property '" + PROXY_PROVIDER_PROPERTY + "'");
			}
			
			try {
				String prop = System.getProperty(PROXY_PROVIDER_PROPERTY);
				if (prop != null) {
					option = ProxyProviderOption.valueOf(prop.toUpperCase());
					
					if (option == null) {
						if (log.isDebugEnabled()) {
							log.debug("Could not determine option from system property '" + PROXY_PROVIDER_PROPERTY + "': " + prop);
						}
					}
					else {
						if (log.isDebugEnabled()) {
							log.debug("Determined option from system property '" + PROXY_PROVIDER_PROPERTY + "': " + option);
						}
					}
				}
				else {
					if (log.isDebugEnabled()) {
						log.debug("The system property '" + PROXY_PROVIDER_PROPERTY + "' was not set, cannot use it to determine which option to use.");
					}
				}
			}
			catch (Exception ex) {
				log.warn("Encountered unexpected failure while attempting to determine which option to use from system property '" + PROXY_PROVIDER_PROPERTY + "'", ex);
			}
			
			if (option == null) {
				if (defaultOption == null) {
					throw new RuntimeException("No default option was provided, cannot determine which option to use for supplying the proxy provider.");
				}
				else {
					if (log.isDebugEnabled()) {
						log.debug("No specific option was chosen, using default option: " + defaultOption);
					}
					option = defaultOption;
				}
			}
		}
		else {
			if (log.isDebugEnabled()) {
				log.debug("Option " + option + " was provided as an argument, using it directly.");
			}
		}
		
		ProxyProvider prov = option.makeProvider();
		ProxyFactory factory = new ProxyFactory(prov);
		
		return factory;
	}
}
