/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring.charts.config;

import static org.jppf.ui.monitoring.charts.ChartType.*;
import static org.jppf.ui.monitoring.data.Fields.*;
import java.util.*;
import javax.swing.*;
import org.jfree.chart.ChartPanel;
import org.jppf.ui.monitoring.charts.*;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.monitoring.event.*;
import org.jppf.ui.utils.GuiUtils;

/**
 * This class is used as a factory to create different charts, as well as for propagating the data updates
 * to all defined charts.
 * @author Laurent Cohen
 */
public class JPPFChartBuilder implements StatsHandlerListener
{
	/**
	 * Singleton instance of this chart builder.
	 */
	private static JPPFChartBuilder instance = null;
	/**
	 * Mapping of chart types to the chart handler used to create and update them.
	 */
	private Map<ChartType, ChartHandler> handlerMap = new HashMap<ChartType, ChartHandler>();
	/**
	 * The tabbed pane in which each pane contains user-defined charts.
	 */
	private JTabbedPane tabbedPane = new JTabbedPane();
	/**
	 * The list of tab names handled by this chart builder.
	 */
	private List<TabConfiguration> tabList = new ArrayList<TabConfiguration>();
	/**
	 * Mapping of tab names to their respective configuration parameters.
	 */
	private Map<String, TabConfiguration> tabMap = new HashMap<String, TabConfiguration>();
	/**
	 * Used to store and retrieve the configuration, to and from the preferences tree.
	 */
	private PreferencesStorage storage = null;

	/**
	 * Initialize this charts builder.
	 */
	protected JPPFChartBuilder()
	{
		storage = new PreferencesStorage();
		initHandlerMap();
	}
	
	/**
	 * Initialize the mapping of chart types to the chart handler used to create and update them.
	 */
	private void initHandlerMap()
	{
		StatsHandler statsHandler = StatsHandler.getInstance();
		handlerMap.put(CHART_PLOTXY, new PlotXYChartHandler(statsHandler));
		handlerMap.put(CHART_3DBAR, new Bar3DChartHandler(statsHandler));
		handlerMap.put(CHART_AREA, new AreaChartHandler(statsHandler));
		handlerMap.put(CHART_3DPIE, new Pie3DChartHandler(statsHandler));
		handlerMap.put(CHART_RING, new RingChartHandler(statsHandler));
		handlerMap.put(CHART_DIFFERENCE, new DifferenceChartHandler(statsHandler));
		//handlerMap.put(CHART_THERMOMETER, new ThermometerChartHandler(statsHandler));
	}

	/**
	 * Create a chart based on a chart configuration.
	 * @param config holds the configuration parameters for the chart created, modified by this method.
	 * @param preview determines whether the configuration should be added to the list of active configurations or not.
	 * A negative value means it's simply appended.
	 * @return the configuration with its created chart set.
	 */
	public ChartConfiguration createChart(ChartConfiguration config, boolean preview)
	{
		ChartHandler handler = handlerMap.get(config.type);
		if (handler == null) return null;
		handler.createChart(config);
		config.chartPanel = new ChartPanel(config.chart);
		return config;
	}

	/**
	 * Remove a tab from the list of tabs.
	 * @param tab the configuration information for the tab to remove.
	 */
	public void removeTab(TabConfiguration tab)
	{
		tabbedPane.remove(tab.panel);
		tabList.remove(tab);
		tabMap.remove(tab.name);
		for (int i=0; i<tabList.size(); i++) tabList.get(i).position = i;
	}
	
	/**
	 * Remove a tab from the list of tabs.
	 * @param tab the configuration information for the tab to remove.
	 */
	public void addTab(TabConfiguration tab)
	{
		tab.panel = GuiUtils.createBoxPanel(BoxLayout.Y_AXIS);
		if (tab.position < 0)
		{
			tabbedPane.addTab(tab.name, tab.panel);
			tabList.add(tab);
		}
		else
		{
			tabbedPane.insertTab(tab.name, null, tab.panel, null, tab.position);
			tabList.add(tab.position, tab);
		}
		tabMap.put(tab.name, tab);
		for (int i=0; i<tabList.size(); i++) tabList.get(i).position = i;
		tab.panel.updateUI();
	}
	
	/**
	 * Remove a specified configuration from the list of active configurations.
	 * @param tab the configuration information for the tab containing the chart.
	 * @param config the configuration to remove.
	 */
	public void removeChart(TabConfiguration tab, ChartConfiguration config)
	{
		tab.configs.remove(config);
		JPanel panel = tab.panel;
		panel.remove(config.chartPanel);
		panel.updateUI();
	}
	
	/**
	 * Update the data displayed in the charts.
	 * @param event holds the new stats values.
	 */
	public void dataUpdated(StatsHandlerEvent event)
	{
		for (TabConfiguration tab: tabMap.values())
		{
			for (final ChartConfiguration config: tab.configs)
			{
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						handlerMap.get(config.type).updateDataset(config);
					}
				});
			}
		}
	}

	/**
	 * Get the tabbed pane in which each pane contains user-defined charts.
	 * @return a <code>JTabbedPane</code> instance.
	 */
	public JTabbedPane getTabbedPane()
	{
		return tabbedPane;
	}

	/**
	 * Get the list of active tabs in this chart builder.
	 * @return a list of tabs names.
	 */
	public List<TabConfiguration> getTabList()
	{
		return tabList;
	}

	/**
	 * Get the tab chartPanel with the specified name.
	 * @param tabName the name of the tab to lookup.
	 * @return a <code>JPanel</code> instance.
	 */
	public JPanel getTabPanel(String tabName)
	{
		return tabMap.get(tabName).panel;
	}

	/**
	 * Create a set of default charts if none is defined.
	 */
	public void createInitialCharts()
	{
		storage.loadChartConfigurations();
		if (tabList.isEmpty()) createDefaultCharts();
	}

	/**
	 * Create a set of default charts if none is defined.
	 */
	public void createDefaultCharts()
	{
		TabConfiguration network = new TabConfiguration("Network", 0);
		addTab(network);
		TabConfiguration plot = new TabConfiguration("Plot Charts", 1);
		addTab(plot);
		TabConfiguration bar = new TabConfiguration("Bar Charts", 2);
		addTab(bar);

		Fields[] fields = new Fields[] { AVG_EXECUTION_TIME, LATEST_EXECUTION_TIME, AVG_NODE_EXECUTION_TIME, LATEST_NODE_EXECUTION_TIME };
		addChart(network, new ChartConfiguration("Execution time", CHART_PLOTXY, "ms", 2, fields));
		fields = new Fields[] { AVG_EXECUTION_TIME, LATEST_EXECUTION_TIME, MAX_EXECUTION_TIME };
		addChart(bar, new ChartConfiguration("Execution time (bar chart)", CHART_3DBAR, "ms", 2, fields));
		addChart(plot, new ChartConfiguration("Execution time", CHART_PLOTXY, "ms", 2, fields));
		fields = new Fields[] { AVG_QUEUE_TIME, LATEST_QUEUE_TIME, MAX_QUEUE_TIME };
		addChart(bar, new ChartConfiguration("Queue time (bar chart)", CHART_3DBAR, "ms", 2, fields));
		addChart(plot, new ChartConfiguration("Queue time", CHART_PLOTXY, "ms", 2, fields));
		fields = new Fields[] { QUEUE_SIZE, MAX_QUEUE_SIZE };
		addChart(bar, new ChartConfiguration("Queue size (bar chart)", CHART_3DBAR, null, 0, fields));
		addChart(plot, new ChartConfiguration("Queue size", CHART_PLOTXY, null, 0, fields));
	}
	
	/**
	 * Add a chart to a tab.
	 * @param tab the tab to add a chart to.
	 * @param config the chart to add.
	 */
	public void addChart(TabConfiguration tab, ChartConfiguration config)
	{
		createChart(config, false);
		if (config.position < 0)
		{
			config.position = tab.configs.size();
			tab.configs.add(config);
			tab.panel.add(config.chartPanel);
		}
		else
		{
			tab.configs.add(config.position, config);
			tab.panel.add(config.chartPanel, config.position);
		}
	}

	/**
	 * Get the object used to store and retrieve the configuration, to and from the preferences tree.
	 * @return a PreferencesStorage object.
	 */
	public PreferencesStorage getStorage()
	{
		return storage;
	}

	/**
	 * Get tingleton instance of this chart builder.
	 * @return a JPPFChartBuilder instance.
	 */
	public static JPPFChartBuilder getInstance()
	{
		if (instance == null) instance = new JPPFChartBuilder();
		return instance;
	}
}
