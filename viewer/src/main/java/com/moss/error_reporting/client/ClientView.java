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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;

public class ClientView extends JPanel {
	private JLabel label;
	private JList list_1;
	private JButton loadReportsListButton;
	private JButton selectByIdButton;
	private JTextArea textArea;
	private JList list;
	public ClientView() {
		super();
		setLayout(new BorderLayout());

		final JSplitPane splitPane = new JSplitPane();
		add(splitPane);

		final JScrollPane scrollPane = new JScrollPane();
		splitPane.setLeftComponent(scrollPane);

		list = new JList();
		scrollPane.setViewportView(list);

		final JPanel panel = new JPanel();
		final GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.rowHeights = new int[] {7,7,7};
		gridBagLayout.columnWidths = new int[] {7};
		panel.setLayout(gridBagLayout);
		splitPane.setRightComponent(panel);

		label = new JLabel();
		label.setText("New JLabel");
		final GridBagConstraints gridBagConstraints_1 = new GridBagConstraints();
		gridBagConstraints_1.insets = new Insets(5, 5, 5, 5);
		gridBagConstraints_1.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints_1.weightx = 1.0;
		gridBagConstraints_1.gridy = 0;
		gridBagConstraints_1.gridx = 0;
		panel.add(label, gridBagConstraints_1);

		final JSplitPane splitPane_1 = new JSplitPane();
		splitPane_1.setDividerLocation(100);
		splitPane_1.setOrientation(JSplitPane.VERTICAL_SPLIT);
		final GridBagConstraints gridBagConstraints_3 = new GridBagConstraints();
		gridBagConstraints_3.insets = new Insets(0, 5, 5, 5);
		gridBagConstraints_3.weighty = 1.0;
		gridBagConstraints_3.fill = GridBagConstraints.BOTH;
		gridBagConstraints_3.gridy = 1;
		gridBagConstraints_3.gridx = 0;
		panel.add(splitPane_1, gridBagConstraints_3);

		final JScrollPane scrollPane_2 = new JScrollPane();
		splitPane_1.setLeftComponent(scrollPane_2);

		list_1 = new JList();
		scrollPane_2.setViewportView(list_1);

		final JScrollPane scrollPane_1 = new JScrollPane();
		splitPane_1.setRightComponent(scrollPane_1);

		textArea = new JTextArea();
		scrollPane_1.setViewportView(textArea);

		final JToolBar toolBar = new JToolBar();
		add(toolBar, BorderLayout.NORTH);

		selectByIdButton = new JButton();
		selectByIdButton.setText("Select By Id");
		toolBar.add(selectByIdButton);

		loadReportsListButton = new JButton();
		loadReportsListButton.setText("Load Reports List");
		toolBar.add(loadReportsListButton);
	}
	public JTextArea getTextArea() {
		return textArea;
	}
	public JList getList() {
		return list;
	}
	public JButton selectByIdButton() {
		return selectByIdButton;
	}
	public JButton loadReportsListButton() {
		return loadReportsListButton;
	}
	public JList partsList() {
		return list_1;
	}
	public JLabel idLabel() {
		return label;
	}

}
