/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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
package org.jppf.ui.monitoring.event;

import java.util.EventListener;

/**
 * Event listener for handling changes to the state of the "ShowIP" toggle.
 * @author Laurent Cohen
 */
public interface ShowIPListener extends EventListener {
  /**
   * Called to notify that the toggle state has changed.
   * @param event the object that encapsulates the toggle change event.
   */
  void stateChanged(ShowIPEvent event);
}
