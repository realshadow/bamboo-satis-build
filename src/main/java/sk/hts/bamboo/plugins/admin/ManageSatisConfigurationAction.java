package sk.hts.bamboo.plugins.admin;

import com.atlassian.bamboo.ww2.BambooActionSupport;
import com.atlassian.bamboo.ww2.aware.permissions.GlobalAdminSecurityAware;
import com.opensymphony.webwork.ServletActionContext;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Admin configuration handler
 */
public class ManageSatisConfigurationAction extends BambooActionSupport implements GlobalAdminSecurityAware {
    private final Logger log = Logger.getLogger(ManageSatisConfigurationAction.class);
    private final SatisConfigManager satisConfigManager;

    private String apiUrl;
    private String vcsUrl;

    public ManageSatisConfigurationAction(SatisConfigManager satisConfigManager) {
        this.satisConfigManager = satisConfigManager;
    }

    /**
     * Endpoint url getter
     *
     * @return endpoint url
     */
    public String getApiUrl() {
        return this.apiUrl;
    }

    /**
     * Endpoint url setter
     *
     * @param apiUrl endpoint url
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * Endpoint url getter
     *
     * @return endpoint url
     */
    public String getVcsUrl() {
        return this.vcsUrl;
    }

    /**
     * Endpoint url setter
     *
     * @param vcsUrl endpoint url
     */
    public void setVcsUrl(String vcsUrl) {
        this.vcsUrl = vcsUrl;
    }

    /**
     * Resolve view method
     *
     * @return action type
     *
     * @throws Exception
     */
    @Override
    public String doExecute() throws Exception {
        HttpServletRequest request = ServletActionContext.getRequest();

        if("POST".equals(request.getMethod())) {
            satisConfigManager.setApiUrl(this.getApiUrl());
            satisConfigManager.setVcsUrl(this.getVcsUrl());

            addActionMessage("Settings were successfully saved.");

            return SUCCESS;
        } else {
            this.setApiUrl(this.satisConfigManager.getApiUrl());
            this.setVcsUrl(this.satisConfigManager.getVcsUrl());

            return INPUT;
        }
    }

    /**
     * POST param validation
     */
    @Override
    public void validate() {
        clearErrorsAndMessages();

        HttpServletRequest request = ServletActionContext.getRequest();

        if("POST".equals(request.getMethod())) {
            if (StringUtils.isBlank(apiUrl)) {
                addFieldError("apiUrl", "Please specify API endpoint URL.");
            } else {
                try {
                    new URL(apiUrl);
                } catch (MalformedURLException mue) {
                    addFieldError("apiUrl", "Please specify a valid API endpoint URL.");
                }
            }

            if (!StringUtils.isBlank(vcsUrl)) {
                try {
                    new URL(vcsUrl);
                } catch (MalformedURLException mue) {
                    addFieldError("vcsUrl", "Please specify a valid VCS URL.");
                }
            }
        }
    }
}
