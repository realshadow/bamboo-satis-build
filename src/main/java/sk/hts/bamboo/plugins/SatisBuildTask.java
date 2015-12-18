package sk.hts.bamboo.plugins;

import com.amazonaws.util.json.JSONArray;
import com.atlassian.bamboo.artifact.Artifact;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.plan.PlanHelper;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskContext;
import com.atlassian.bamboo.deployments.execution.DeploymentTaskType;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.repository.Repository;
import com.atlassian.bamboo.task.*;
import com.atlassian.bamboo.build.logger.interceptors.ErrorMemorisingInterceptor;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.request.HttpRequestWithBody;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.NotNull;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import sk.hts.bamboo.plugins.admin.SatisConfigManager;
import com.atlassian.bamboo.variable.CustomVariableContext;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Satis task for calling API endpoint with repository URL and package name
 */
public class SatisBuildTask implements DeploymentTaskType
{
    private final PlanManager planManager;
    private final SatisConfigManager satisConfigManager;
    private CustomVariableContext customVariableContext;

    private final static String PACKAGE_FILE = "composer.json";
    private final static String SSH_PREFIX = "ssh://";

    /**
     * MD5 encoder
     *
     * @param input string to be hashed
     *
     * @return hashed string
     *
     * @throws NoSuchAlgorithmException
     */
    protected String md5(String input) throws NoSuchAlgorithmException
    {
        String result = input;

        if(input != null) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());

            BigInteger hash = new BigInteger(1, md.digest());

            result = hash.toString(16);
            while(result.length() < 32) {
                result = "0" + result;
            }
        }

        return result;
    }

    /**
     * Constructor
     *
     * @param planManager plan manager
     */
    public SatisBuildTask(PlanManager planManager, SatisConfigManager satisConfigManager,
                          CustomVariableContext customVariableContext
    )
    {
        this.planManager = planManager;
        this.satisConfigManager = satisConfigManager;
        this.customVariableContext = customVariableContext;
    }

    /**
     *
     * @param deploymentTaskContext task context
     * @return task execution result result
     */
    @NotNull
    @Override
    public TaskResult execute(@NotNull final DeploymentTaskContext deploymentTaskContext)
    {
        final ErrorMemorisingInterceptor errorLines = ErrorMemorisingInterceptor.newInterceptor();
        deploymentTaskContext.getBuildLogger().getInterceptorStack().add(errorLines);

        try {
            final TaskResultBuilder taskResultBuilder = TaskResultBuilder.newBuilder(deploymentTaskContext);
            final BuildLogger buildLogger = deploymentTaskContext.getBuildLogger();
            final Map<String, Artifact> artifactMap = deploymentTaskContext.getDeploymentContext()
                    .getVersionArtifacts();
            String satisApiUrl = this.satisConfigManager.getApiUrl();

            if (satisApiUrl.length() == 0) {
                buildLogger.addErrorLogEntry("Satis API endpoint URL is empty. Did you forget to set it?");

                return taskResultBuilder.failedWithError().build();
            }

            if (artifactMap.size() == 0) {
                buildLogger.addErrorLogEntry("No artifacts were found for this build. Did you forget to " +
                        "export " + PACKAGE_FILE + " artifact?");

                return taskResultBuilder.failedWithError().build();
            }

            // -- we do this just so we can get to plan settings and thus repository URL
            final Artifact artifact = artifactMap.entrySet().iterator().next().getValue();
            final Plan plan = this.planManager.getPlanByKey(artifact.getPlanResultKey().getPlanKey());

            if(plan == null) {
                buildLogger.addErrorLogEntry("Unable to find build plan for " + PACKAGE_FILE + " artifact.");

                return taskResultBuilder.failedWithError().build();
            }

            final String filePath = deploymentTaskContext.getWorkingDirectory().getPath();

            File f = new File(filePath, PACKAGE_FILE);
            if (!f.exists() || f.isDirectory()) {
                buildLogger.addErrorLogEntry("Required artifact \""+ PACKAGE_FILE + "\" was not found.");

                return taskResultBuilder.failedWithError().build();
            }

            String packageJson;
            try {
                packageJson = new String(Files.readAllBytes(f.toPath()));
            } catch(IOException e) {
                buildLogger.addErrorLogEntry("Unable to open " + PACKAGE_FILE + " (" + e.getMessage() + ").");

                return taskResultBuilder.failedWithError().build();
            }

            String packageName;
            try {
                JsonObject json = new JsonParser().parse(packageJson).getAsJsonObject();
                packageName = json.get("name").getAsString();
            } catch(JsonIOException | JsonSyntaxException e) {
                buildLogger.addErrorLogEntry("Unable to parse " + PACKAGE_FILE + ". (" + e.getMessage() + ").");

                return taskResultBuilder.failedWithError().build();
            }

            // nullPointer potential
            Repository defaultRepository = PlanHelper.getDefaultRepository(plan);
            HierarchicalConfiguration configuration = defaultRepository.toConfiguration();

            String repositoryUrl = "";
            if ("com.atlassian.bamboo.plugins.stash.StashRepository".equals(defaultRepository.getClass().getName())) {
                String vcsUrl = this.satisConfigManager.getVcsUrl();
                repositoryUrl = configuration.getString("repository.stash.repositoryUrl");

                if (repositoryUrl.startsWith(SSH_PREFIX) && !vcsUrl.isEmpty()) {
                    if (vcsUrl.endsWith("/")) {
                        vcsUrl = vcsUrl.substring(0, vcsUrl.length() - 1);
                    }

                    repositoryUrl = repositoryUrl.replaceAll("ssh:\\/\\/.+\\d", vcsUrl);
                }
            } else if ("com.atlassian.bamboo.plugins.git.GitRepository".equals(defaultRepository.getClass().getName())) {
                repositoryUrl =  this.customVariableContext.substituteString(
                        configuration.getString("repository.git.repositoryUrl")
                );
            } else {
                buildLogger.addErrorLogEntry("Only Git and Stash repositories are currently supported.");

                return taskResultBuilder.failedWithError().build();
            }

            if (repositoryUrl.isEmpty()) {
                buildLogger.addErrorLogEntry("Repository URL could not be determined.");

                return taskResultBuilder.failedWithError().build();
            }

            if (satisApiUrl.endsWith("/")) {
                satisApiUrl = satisApiUrl.substring(0, satisApiUrl.length() - 1);
            }

            satisApiUrl += "/{repositoryId}";

            try {
                HttpRequestWithBody request = Unirest.put(satisApiUrl)
                        .routeParam("repositoryId", this.md5(repositoryUrl));

                buildLogger.addBuildLogEntry("Requesting: " + request.getUrl());
                buildLogger.addBuildLogEntry("Detected repository URL: " + repositoryUrl);

                buildLogger.addBuildLogEntry("Triggering rebuild via API...");

                HttpResponse<JsonNode> httpResponse = request.field("url", repositoryUrl)
                        .field("package_name", packageName)
                        .field("async_mode", false)
                        .asJson();
                if (httpResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                    throw new HttpResponseException(httpResponse.getStatus(), "Bad response from API. Check logs for details.");
                } else {
                    org.json.JSONArray commandOutput = httpResponse.getBody().getObject().getJSONArray("command_output");

                    for(int i = 0; i < commandOutput.length(); i++) {
                        buildLogger.addBuildLogEntry(commandOutput.getString(i));
                    }

                    buildLogger.addBuildLogEntry("");
                }

                return taskResultBuilder.build();
            } catch (NoSuchAlgorithmException e) {
                buildLogger.addErrorLogEntry("Unable to build repository URL: (" + e.getMessage() + ")");
            } catch (UnirestException e) {
                buildLogger.addErrorLogEntry("Unable create/parse request to API: (" + e.getMessage() + ")");
            } catch(HttpResponseException e) {
                if (e.getStatusCode() == Response.Status.NOT_FOUND.getStatusCode()) {
                    buildLogger.addErrorLogEntry("Repository does not exist. Did you forget to " +
                            "register in control panel?");
                } else {
                    buildLogger.addErrorLogEntry("Unexpected HTTP exception " + e.getStatusCode() + " found, " +
                            "check API logs for details.");
                }
            }

            return taskResultBuilder.failedWithError().build();
        } finally {
            deploymentTaskContext.getCommonContext().getCurrentResult().addBuildErrors(errorLines.getErrorStringList());
        }
    }
}