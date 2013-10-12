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
package com.moss.error_reporting.client;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.joda.time.DateTime;

import com.moss.error_reporting.api.ErrorReport;
import com.moss.error_reporting.api.ErrorReportChunk;
import com.moss.error_reporting.api.ErrorReportSummary;
import com.moss.error_reporting.api.ReportId;
import com.moss.error_reporting.api.ReportingService;
import com.moss.rpcutil.proxy.ProxyFactory;

@SuppressWarnings("serial")
public final class Client extends JPanel {

	private static String serviceUrlString = "http://localhost:6008/Reporting?wsdl";
	
	public static void main(String[] args) {
		try {
			URL serviceUrl = new URL(serviceUrlString);
			
			ProxyFactory pf = ErrorReportProxyFactory.create();
			ReportingService reportingService = pf.create(ReportingService.class, serviceUrl);
			
			JFrame frame = new JFrame("Error Reports Viewer");
			frame.getContentPane().add(new Client(reportingService));
			frame.setSize(640, 480);
			frame.setLocationRelativeTo(null);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
			
		} catch(Exception ex) {
			ex.printStackTrace();
		}	
	}
	 
	private final ClientView view = new ClientView();
	private final ReportingService reportingService;
	
	public Client(final ReportingService reportingService) {
		setLayout(new BorderLayout());
		add(view);
		this.reportingService = reportingService;
		
		view.selectByIdButton().addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e) {
			UUID id = UUID.fromString(JOptionPane.showInputDialog(view, "Enter UUID"));
			ErrorReport report = reportingService.getReport(new ReportId(id));
			showReport(report);
		}});
		
		view.loadReportsListButton().addActionListener(new ActionListener(){public void actionPerformed(ActionEvent e) {
			new ListReportsThread().start();
		}});
		
		view.getList().setCellRenderer(new DefaultListCellRenderer(){
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				return super.getListCellRendererComponent(list, ((ErrorReportSummary)value).getWhenReceived(), index, isSelected,
						cellHasFocus);
			}
		});
		
		view.getList().addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				else if (view.getList().getSelectedValue() == null) {
					view.partsList().setModel(new DefaultListModel());
					view.getTextArea().setText("");
				}
				else {
					showReport(((ErrorReportSummary)view.getList().getSelectedValue()).getId());
				}
			}
		});
		
		view.partsList().addListSelectionListener(new ListSelectionListener(){public void valueChanged(ListSelectionEvent e) {
			if(e.getValueIsAdjusting()) return;
			ErrorReportChunk chunk = (ErrorReportChunk) view.partsList().getSelectedValue();
			new DisplayChunkThread(chunk).start();
		}});
		
	}
	
	private JDialog dialog;
	
	private final class DisplayChunkThread extends Thread {
		private ErrorReportChunk chunk;
		
		private DisplayChunkThread(ErrorReportChunk chunk) {
			super();
			this.chunk = chunk;
		}

		@Override
		public void run() {
			String text = "";
			if(chunk!=null){
				showProgressDialog("Showing Chunk");
				try {
					ByteArrayOutputStream bytes = new ByteArrayOutputStream();
					PrintStream out = new PrintStream(bytes);
					print(chunk, out);
					out.close();
					bytes.close();
					text = new String(bytes.toByteArray());
				} catch (IOException error) {
					error.printStackTrace();
				} finally{
					hideProgressDialog();
				}
				view.getTextArea().setText(text);
				view.getTextArea().setCaretPosition(0);
			}
		}
	}
	
	private class ListReportsThread extends Thread {
		@Override
		public void run() {
			showProgressDialog("Retrieving error reports list");
			System.out.println("Loading ");
			List<ErrorReportSummary> summaries = reportingService.listReportSummaries();
			Collections.sort(summaries, new Comparator<ErrorReportSummary>(){
				public int compare(ErrorReportSummary o1, ErrorReportSummary o2) {
					return -o1.getWhenReceived().compareTo(o2.getWhenReceived());
				}
			});
			System.out.println("Done (" + summaries.size() + ")");
			DefaultListModel list = new DefaultListModel();
			for(ErrorReportSummary s: summaries){
				list.addElement(s);
			}
			view.getList().setModel(list);
			hideProgressDialog();
		}
	}
	
	private synchronized void showProgressDialog(String title){
		if(dialog!=null){
			dialog.setTitle(title);
			return;
		}
		dialog = new JDialog((Frame)SwingUtilities.windowForComponent(view), title);
		JProgressBar pbar = new JProgressBar();
		pbar.setIndeterminate(true);
		dialog.getContentPane().add(pbar);
		dialog.setLocationRelativeTo(view);
		dialog.pack();
		dialog.setSize(300, dialog.getHeight());
		dialog.setModal(true);
		new Thread(){
			@Override
			public void run() {
				dialog.setVisible(true);
			}
		}.start();
	}
	
	private synchronized void hideProgressDialog(){
		if(dialog!=null){
			while(!dialog.isVisible()){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			dialog.dispose();
			dialog=null;
		}
	}
	
	private final class ErrorReportDisplayProcess implements Runnable {
		private ErrorReport errorReport;
		private ReportId id;
		
		public ErrorReportDisplayProcess(ErrorReport errorReport) {
			super();
			this.errorReport = errorReport;
		}
		public ErrorReportDisplayProcess(ReportId id){
			this.id = id;
		}
		
		public void run() {
			System.out.println("Error report download/display starting...");
			showProgressDialog("Downloading Error Report ");
			
			if(errorReport==null){
				System.out.println("Downloading error report");
				errorReport = reportingService.getReport(id);
				System.out.println("Downloaded");
			}
			DefaultListModel list = new DefaultListModel();
			for(ErrorReportChunk chunk : errorReport.getReportChunks()){
				list.addElement(chunk);
			}
			view.partsList().setModel(list);
			view.partsList().setCellRenderer(new DefaultListCellRenderer(){
				@Override
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {
					ErrorReportChunk chunk = ((ErrorReportChunk)value);
					return super.getListCellRendererComponent(list, chunk.getName() + " " + chunk.getMimeType() + " " + (chunk.getData().length/1024) + "k", index, isSelected,
							cellHasFocus);
				}
			});
			view.getTextArea().setText("");
			String title = errorReport.getWhenReceived()==null?"Unknown":errorReport.getWhenReceived().toDateTime(new DateTime().getZone()).toString();
			view.idLabel().setText("Received " + title);
			
			System.out.println("...Error report download/display done.");
			hideProgressDialog();
		}
	}
	
	private void showReport(ErrorReport errorReport){
		
		new Thread(new ErrorReportDisplayProcess(errorReport)).start();
	}
	
	private void showReport(ReportId reportId){
		new Thread(new ErrorReportDisplayProcess(reportId)).start();
	}
	
	private static void print(ErrorReportChunk chunk, PrintStream output) throws IOException {
			if(chunk.getMimeType()==null){
				output.println("ERROR: No Mime Type");
				return;
			}
//			output.println("Chunk \"" + chunk.getName() + "\" (" + chunk.getMimeType() + ")");
			if(chunk.getMimeType().equals("text/plain"))
				output.println(new String(chunk.getData()));
			else if(chunk.getMimeType().equals("application/zip")){
				ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(chunk.getData()));
				for(ZipEntry entry = in.getNextEntry();entry!=null;entry = in.getNextEntry()){
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					byte[] buffer = new byte[1024*1024];
					for(int numRead = in.read(buffer);numRead!=-1;numRead = in.read(buffer)){
						out.write(buffer, 0, numRead);
					}
					out.close();
					output.println("Zip Entry:" + entry.getName() + ":\n" + new String(out.toByteArray()));
				}
		}
	}
}
