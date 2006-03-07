/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring;

import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import org.apache.log4j.Logger;
import org.jppf.server.JPPFStats;
import org.jppf.ui.monitoring.charts.*;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.event.*;
import org.jvnet.substance.*;

/**
 * This class provides a graphical interface for monitoring the status and health 
 * of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) fot the whole UI.
 * @author Laurent Cohen
 */
public class MonitoringPanel extends JPanel implements StatsHandlerListener, StatsConstants
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(MonitoringPanel.class);
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsHandler = null;
	/**
	 * Holds a list of table models to update wwhen new stats are received.
	 */
	private List<MonitorTableModel> tableModels = new ArrayList<MonitorTableModel>();

	/**
	 * Default contructor.
	 * @param statsHandler the stats formatter that provides the data.
	 */
	public MonitoringPanel(StatsHandler statsHandler)
	{
		this.statsHandler = statsHandler;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(Box.createVerticalStrut(5));
		add(makeRefreshPanel());
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(EXECUTION_PROPS, "Execution"));
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(NODE_EXECUTION_PROPS, "Node Execution"));
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(TRANSPORT_PROPS, "Network ovehead"));
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(QUEUE_PROPS, "Queue"));
		add(Box.createVerticalStrut(5));
		add(makeTablePanel(CONNECTION_PROPS, "Connections"));
		//add(Box.createVerticalStrut(5));
		add(Box.createVerticalGlue());
	}
	
	/**
	 * Called when new stats have been received from the server.
	 * @param event holds the new stats values.
	 */
	public void dataUpdated(StatsHandlerEvent event)
	{
		for (final MonitorTableModel model: tableModels)
		{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					model.fireTableDataChanged();
				}
			});
		}
	}
	
	/**
	 * Create a chartPanel with a &quot;refresh now&quote; button.
	 * @return a <code>JComponent</code> instance.
	 */
	private JComponent makeRefreshPanel()
	{
		JButton btn = new JButton("Refresh Now");
		btn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				statsHandler.requestUpdate();
			}
		});
		btn.setPreferredSize(new Dimension(100, 20));
		return btn;
	}

	/**
	 * Create a chartPanel displaying a group of values.
	 * @param props the names of the values to display.
	 * @param title the title of the chartPanel.
	 * @return a <code>JComponent</code> instance.
	 */
	private JComponent makeTablePanel(String[] props, String title)
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		panel.setBorder(BorderFactory.createTitledBorder(title));
		JTable table = new JTable()
		{
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		};
		MonitorTableModel model = new MonitorTableModel(props);
		table.setModel(model);
		table.setOpaque(false);
		DefaultTableCellRenderer rend1 = new SubstanceDefaultTableCellRenderer();
		rend1.setHorizontalAlignment(JLabel.RIGHT);
		rend1.setOpaque(false);
		table.getColumnModel().getColumn(1).setCellRenderer(rend1);
		DefaultTableCellRenderer rend0 = new SubstanceDefaultTableCellRenderer();
		rend0.setHorizontalAlignment(JLabel.LEFT);
		rend0.setOpaque(false);
		table.getColumnModel().getColumn(0).setCellRenderer(rend0);
		tableModels.add(model);
		panel.add(table);
		table.setShowGrid(false);
		return panel;
	}

	/**
	 * Data model for the tables displaying the values.
	 */
	private class MonitorTableModel extends AbstractTableModel
	{
		/**
		 * The list of fields whose values are displayed in the table.
		 */
		private String[] fields = null;

		/**
		 * Initialize this table model witht he specified list of fields.
		 * @param fields the list of fields whose values are displayed in the table.
		 */
		MonitorTableModel(String[] fields)
		{
			this.fields = fields;
		}

		/**
		 * Get the number of columns in the table.
		 * @return 2.
		 * @see javax.swing.table.TableModel#getColumnCount()
		 */
		public int getColumnCount()
		{
			return 2;
		}

		/**
		 * Get the number of rows in the table.
		 * @return the number of fields displayed in the table.
		 * @see javax.swing.table.TableModel#getRowCount()
		 */
		public int getRowCount()
		{
			return fields.length;
		}

		/**
		 * Get a value at specified coordinates in the table.
		 * @param row the row coordinate.
		 * @param column the column coordinate.
		 * @return the value as an object.
		 * @see javax.swing.table.TableModel#getValueAt(int, int)
		 */
		public Object getValueAt(int row, int column)
		{
			Map<String, String> valuesMap = null;
			if (statsHandler.getStatsCount() > 0) valuesMap = statsHandler.getLatestStringValues();
			else valuesMap = StatsFormatter.getStringValuesMap(new JPPFStats());
			String name = fields[row];
			return column == 0 ? name : valuesMap.get(name);
		}
	}

	/**
	 * Start this UI.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
			SubstanceLookAndFeel.setCurrentWatermark(new TiledImageWatermark("org/jppf/ui/resources/GridWatermark.gif"));
			SubstanceLookAndFeel.setCurrentTheme(new JPPFTheme(new JPPFColorScheme(), "JPPF", false));
			StatsHandler statsHandler = new StatsHandler();
			JFrame frame = new JFrame("Test");
			JTabbedPane tabbedPane = new JTabbedPane();
			MonitoringPanel monitor = new MonitoringPanel(statsHandler);
			AdminPanel admin = new AdminPanel(statsHandler);
			JPPFChartBuilder builder = new JPPFChartBuilder(statsHandler);
			builder.createInitialCharts();
			ChartConfigurationPanel configPanel = new ChartConfigurationPanel(builder);
			statsHandler.addStatsHandlerListener(monitor);
			statsHandler.addStatsHandlerListener(builder);
			tabbedPane.addTab("Server Stats", monitor);
			tabbedPane.addTab("Charts", builder.getTabbedPane());
			tabbedPane.addTab("Charts config", configPanel);
			tabbedPane.addTab("Admin", admin);
			tabbedPane.addTab("Options", new OptionsPanel(statsHandler));
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					System.exit(0);
				}
			});
			frame.add(tabbedPane);
			frame.setSize(600, 600);
			frame.setVisible(true);
			statsHandler.startRefreshTimer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
