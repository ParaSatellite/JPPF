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
package org.jppf.ui.monitoring.event;

import java.util.EventObject;
import org.jppf.ui.monitoring.data.*;

/**
 * Event sent when the stats data has changed.
 * @author Laurent Cohen
 */
public class StatsHandlerEvent extends EventObject
{

	/**
	 * Initialize this event with a specified source <code>StatsHandler</code>.
	 * @param source the stats formatter whose data has changed.
	 */
	public StatsHandlerEvent(StatsConstants source)
	{
		super(source);
	}
	
	/**
	 * Get the <code>StatsHandler</code> source of this event.
	 * @return a <code>StatsHandler</code> instance.
	 */
	public StatsHandler getStatsFormatter()
	{
		return (StatsHandler) getSource();
	}
}
