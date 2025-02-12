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
package com.osgifx.console.bnd.dp.packager;

import java.util.Collection;

public final class DeploymentPackageDTO {

    public String                                symbolicName;
    public String                                version;
    public String                                fixPack;
    public String                                name;
    public String                                copyright;
    public String                                contactAddress;
    public String                                description;
    public String                                docURL;
    public String                                icon;
    public String                                vendor;
    public String                                license;
    public String                                requiredStorage;
    public String                                isCustomizer;
    public Collection<DeploymentPackageEntryDTO> entries;

}
