/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.serialization;

import java.io.*;
import java.io.ObjectOutputStream.PutField;
import java.util.*;

/**
 * JPPF implementation of the PutField API.
 * @author Laurent Cohen
 */
class JPPFPutField extends PutField
{
	/**
	 * Map of names to primitive values.
	 */
	Map<String, Object> primitiveFields = new HashMap<String, Object>();
	/**
	 * Map of names to object values.
	 */
	Map<String, Object> objectFields = new HashMap<String, Object>();
	/**
	 * The stream to write to.
	 */
	ObjectOutputStream out;

	/**
	 * Initialize this put field with the specified class descriptor.
	 * @param out the stream to write the fields to.
	 */
	public JPPFPutField(ObjectOutputStream out)
	{
		this.out = out;
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, boolean val)
	{
		primitiveFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, byte val)
	{
		primitiveFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, char val)
	{
		primitiveFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, short val)
	{
		primitiveFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, int val)
	{
		primitiveFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, long val)
	{
		primitiveFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, float val)
	{
		primitiveFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, double val)
	{
		primitiveFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void put(String name, Object val)
	{
		objectFields.put(name, val);
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(ObjectOutput out) throws IOException
	{
		if (out != this.out) throw new IOException("not using the right stream");
		this.out.writeFields();
	}
}
