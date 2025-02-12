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
package com.osgifx.console.application.dialog;

import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.MoreObjects;

public final class SocketConnectionSettingDTO {

    public String id;
    public String name;
    public String host;
    public int    port;
    public int    timeout;
    public String trustStorePath;
    public String trustStorePassword;

    public SocketConnectionSettingDTO() {
        // needed for GSON
    }

    public SocketConnectionSettingDTO(final String name,
                                      final String host,
                                      final int port,
                                      final int timeout,
                                      final String trustStorePath,
                                      final String trustStorePassword) {
        this(UUID.randomUUID().toString(), name, host, port, timeout, trustStorePath, trustStorePassword);
    }

    public SocketConnectionSettingDTO(final String id,
                                      final String name,
                                      final String host,
                                      final int port,
                                      final int timeout,
                                      final String trustStorePath,
                                      final String trustStorePassword) {
        this.id                 = id;
        this.name               = name;
        this.host               = host;
        this.port               = port;
        this.timeout            = timeout;
        this.trustStorePath     = trustStorePath;
        this.trustStorePassword = trustStorePassword;
    }

    @Override
    public int hashCode() {
        // @formatter:off
        return new HashCodeBuilder()
                         .append(id)
                         .append(name)
                         .append(host)
                         .append(port)
                         .append(timeout)
                         .append(trustStorePath)
                         .append(trustStorePassword)
                     .toHashCode();
        // @formatter:on
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final var other = (SocketConnectionSettingDTO) obj;
        // @formatter:off
        return new EqualsBuilder()
                           .append(id, other.id)
                           .append(name, other.name)
                           .append(host, other.host)
                           .append(port, other.port)
                           .append(timeout, other.timeout)
                           .append(trustStorePassword, other.trustStorePassword)
                           .append(trustStorePath, other.trustStorePath)
                       .isEquals();
        // @formatter:on
    }

    @Override
    public String toString() {
        // @formatter:off
        return MoreObjects.toStringHelper(getClass())
                               .add("id", id)
                               .add("name", name)
                               .add("host", host)
                               .add("port", port)
                               .add("timeout", timeout)
                               .add("trustStorePath", trustStorePath)
                          .toString();
        // @formatter:on
    }

}
