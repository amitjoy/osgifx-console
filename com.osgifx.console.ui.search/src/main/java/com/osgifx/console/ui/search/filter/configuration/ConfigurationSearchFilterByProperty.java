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
package com.osgifx.console.ui.search.filter.configuration;

import static com.osgifx.console.ui.search.filter.SearchComponent.CONFIGURATIONS;
import static com.osgifx.console.ui.search.filter.SearchOperation.EQUALS_TO;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.component.annotations.Component;

import com.dlsc.formsfx.model.validators.CustomValidator;
import com.dlsc.formsfx.model.validators.Validator;
import com.google.common.base.Splitter;
import com.google.common.base.VerifyException;
import com.google.common.collect.Iterables;
import com.google.mu.util.stream.BiStream;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.ui.search.filter.SearchComponent;
import com.osgifx.console.ui.search.filter.SearchFilter;
import com.osgifx.console.ui.search.filter.SearchOperation;

@Component
public final class ConfigurationSearchFilterByProperty implements SearchFilter {

    @Override
    public Predicate<XConfigurationDTO> predicate(final String input, final SearchOperation searchOperation) {
        final var split = Splitter.on("=").trimResults().splitToList(input.strip());
        final var key   = split.get(0);
        final var value = split.get(1);

        return switch (searchOperation) {
            case EQUALS_TO -> configuration -> //
                BiStream.from(configuration.properties) //
                        .anyMatch((k, v) -> //
                StringUtils.equalsIgnoreCase(v.key, key) && StringUtils.equalsIgnoreCase(v.value.toString(), value));
            default -> throw new VerifyException("no matching case found");
        };
    }

    @Override
    public Collection<SearchOperation> supportedOperations() {
        return List.of(EQUALS_TO);
    }

    @Override
    public SearchComponent component() {
        return CONFIGURATIONS;
    }

    @Override
    public String placeholder() {
        return "PropertyName=PropertyValue Format (Case-Insensitive)";
    }

    @Override
    public Validator<String> validator() {
        return CustomValidator.forPredicate(e -> Iterables.size(Splitter.on("=").split(e.strip())) == 2,
                "Invalid Format -> Allowed Format: Key=Value");
    }

    @Override
    public String toString() {
        return "Property";
    }

}
