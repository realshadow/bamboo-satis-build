/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sk.hts.bamboo.plugins.admin;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import org.apache.commons.lang.StringUtils;

/**
 * Config manager
 */
public class SatisConfigManager {
    private static final String SETTINGS_NAMESPACE = "sk.hts.bamboo.plugins.admin.satis-build.";
    private static final String API_URL_KEY = "apiUrl";
    private static final String VCS_URL_KEY = "vcsUrl";

    protected final PluginSettingsFactory pluginSettingsFactory;

    public SatisConfigManager(final PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    /**
     * Satis control panel URL getter
     *
     * @return control panel URL
     */
    public String getApiUrl() {
        Object apiUrl = pluginSettingsFactory.createGlobalSettings().get(SETTINGS_NAMESPACE + API_URL_KEY);

        return (apiUrl instanceof String ? (String) apiUrl : "");
    }

    /**
     * Satis control panel URL setter
     *
     * @param apiUrl - control panel URL
     */
    public void setApiUrl(String apiUrl) {
        if (StringUtils.isBlank(apiUrl)) {
            pluginSettingsFactory.createGlobalSettings().remove(SETTINGS_NAMESPACE + API_URL_KEY);
        } else {
            pluginSettingsFactory.createGlobalSettings().put(SETTINGS_NAMESPACE + API_URL_KEY, apiUrl);
        }
    }

    /**
     * VCS URL getter
     *
     * @return VCS URL
     */
    public String getVcsUrl() {
        Object vcsUrl = pluginSettingsFactory.createGlobalSettings().get(SETTINGS_NAMESPACE + VCS_URL_KEY);

        return (vcsUrl instanceof String ? (String) vcsUrl : "");
    }

    /**
     * VCS URL setter
     *
     * @param vcsUrl - VCS URL
     */
    public void setVcsUrl(String vcsUrl) {
        if (StringUtils.isBlank(vcsUrl)) {
            pluginSettingsFactory.createGlobalSettings().remove(SETTINGS_NAMESPACE + VCS_URL_KEY);
        } else {
            pluginSettingsFactory.createGlobalSettings().put(SETTINGS_NAMESPACE + VCS_URL_KEY, vcsUrl);
        }
    }
}