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

package org.jppf.utils.streams.serialization;

import java.io.*;

import org.jppf.serialization.JPPFObjectStreamBuilder;

/**
 * 
 * @author Laurent Cohen
 */
public class ObjectStreamBuilderImpl implements JPPFObjectStreamBuilder
{

	/**
	 * {@inheritDoc}
	 */
	public ObjectInputStream newObjectInputStream(InputStream in) throws Exception
	{
		return new JPPFObjectInputStream(in);
	}

	/**
	 * {@inheritDoc}
	 */
	public ObjectOutputStream newObjectOutputStream(OutputStream out) throws Exception
	{
		return new JPPFObjectOutputStream(out);
	}
}
