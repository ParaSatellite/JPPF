/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.ui.monitoring.charts;

import org.jppf.ui.monitoring.data.StatsHandler;

/**
 * Instances of this class are used to create and update line charts with an horizontal orientation.
 * @author Laurent Cohen
 */
public class StackedBarSeries3DChartHandler extends BarSeries3DChartHandler {
  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   */
  public StackedBarSeries3DChartHandler(final StatsHandler statsHandler) {
    super(statsHandler,  "createStackedBarChart3D");
  }
}