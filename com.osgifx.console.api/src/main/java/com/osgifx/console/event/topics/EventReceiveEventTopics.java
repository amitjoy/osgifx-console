/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.event.topics;

public final class EventReceiveEventTopics {

    private EventReceiveEventTopics() {
        throw new IllegalAccessError("Cannot be instantiated");
    }

    public static final String CLEAR_EVENTS_TOPIC = "com/osgifx/clear/events";

    public static final String EVENT_RECEIVE_EVENT_TOPIC_PREFIX  = "osgi/fx/event/receive/";
    public static final String EVENT_RECEIVE_EVENT_TOPICS        = EVENT_RECEIVE_EVENT_TOPIC_PREFIX + "*";
    public static final String EVENT_RECEIVE_STARTED_EVENT_TOPIC = EVENT_RECEIVE_EVENT_TOPIC_PREFIX + "started";
    public static final String EVENT_RECEIVE_STOPPED_EVENT_TOPIC = EVENT_RECEIVE_EVENT_TOPIC_PREFIX + "stopped";

}
