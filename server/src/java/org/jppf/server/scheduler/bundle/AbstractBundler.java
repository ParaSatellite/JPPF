/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

package org.jppf.server.scheduler.bundle;

/**
 * Abstract implementation of the bundler interface.
 * @author Laurent Cohen
 */
public abstract class AbstractBundler implements Bundler
{
	/**
	 * Count of the bundlers used to generate a readable unique id.
	 */
	protected static int bundlerCount = 0;
	/**
	 * The bundler number for this bundler.
	 */
	protected int bundlerNumber = incBundlerCount();

	/**
	 * The creation timestamp for this bundler.
	 */
	protected long timestamp = System.currentTimeMillis();
	/**
	 * The override indicator.
	 */
	protected boolean override = false;

	/**
	 * Increment the bundlers count by one.
	 * @return the new count as an int value.
	 */
	private static synchronized int incBundlerCount()
	{
		return ++bundlerCount;
	}

	/**
	 * This method does nothing and should be overriden in subclasses.
	 * @param bundleSize not used.
	 * @param totalTime not used.
	 * @see org.jppf.server.scheduler.bundle.Bundler#feedback(int, double)
	 */
	public void feedback(int bundleSize, double totalTime)
	{
	}

	/**
	 * Get the timestamp at which this bundler was created.
	 * This is used to enable node channels to know when the bundler settings have changed.
	 * @return the timestamp as a long value.
	 * @see org.jppf.server.scheduler.bundle.Bundler#getTimestamp()
	 */
	public long getTimestamp()
	{
		return timestamp;
	}

	/**
	 * Get the  override indicator.
	 * @return true if the settings were overriden by the node, false otherwise.
	 * @see org.jppf.server.scheduler.bundle.Bundler#isOverride()
	 */
	public boolean isOverride()
	{
		return override;
	}

	/**
	 * Set the  override indicator.
	 * @param override true if the settings were overriden by the node, false otherwise.
	 */
	public void setOverride(boolean override)
	{
		this.override = override;
	}
}
