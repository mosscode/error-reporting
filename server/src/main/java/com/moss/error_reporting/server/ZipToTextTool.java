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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ZipToTextTool {
	public void run(ZipInputStream in, StringBuffer body) throws IOException {

		ZipEntry entry = in.getNextEntry();
		while(entry!=null){
			if(entry.getName().toLowerCase().endsWith(".log")){
				body.append("\n");
				body.append("-----ZIP ENTRY \"" + entry.getName() + "\"------");
				body.append("\n");
				body.append("\n");
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				for(String line = reader.readLine();line!=null;line = reader.readLine()){
					body.append(line);
					body.append('\n');
				}
			}else{
				body.append("\n");
				body.append("-----ZIP ENTRY \"" + entry.getName() + "\" NOT DISPLAYED-----");
				body.append("\n");
			}
			in.closeEntry();
			entry = in.getNextEntry();
		}
	}
}
