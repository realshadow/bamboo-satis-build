package sk.hts.bamboo.plugins;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.BuildTaskRequirementSupport;
import com.atlassian.bamboo.task.TaskConfigConstants;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.ww2.actions.build.admin.create.UIConfigSupport;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Task configurator
 */
public class SatisBuildTaskConfigurator extends AbstractTaskConfigurator implements BuildTaskRequirementSupport
{
    protected static final Set<String> FIELDS = Sets.newHashSet();

    public static final String CTX_UI_CONFIG_BEAN = "uiConfigSupport";

    public com.opensymphony.xwork2.TextProvider textProvider;
    public UIConfigSupport uiConfigSupport;

    static {
        FIELDS.add(TaskConfigConstants.CFG_WORKING_SUB_DIRECTORY);
    }

    /**
     * Generate config map
     *
     * @param params action params
     * @param previousTaskDefinition previous task config
     * @return config map
     */
    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull ActionParametersMap params, @Nullable TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);
        taskConfiguratorHelper.populateTaskConfigMapWithActionParameters(
            config, params, FIELDS
        );

        return config;
    }

    /**
     * Populate fields for task creation
     *
     * @param context task context
     */
    @Override
    public void populateContextForCreate(@NotNull Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        populateContextForAllOperations(context);
    }

    /**
     * Populate fields for task edit
     *
     * @param context task context
     */
    @Override
    public void populateContextForEdit(@NotNull Map<String, Object> context, @NotNull TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        populateContextForAllOperations(context);

        taskConfiguratorHelper.populateContextWithConfiguration(
                context, taskDefinition, FIELDS
        );
    }

    /**
     * Populate fields for every operation
     *
     * @param context task context
     */
    public void populateContextForAllOperations(@NotNull Map<String, Object> context)
    {
        context.put(CTX_UI_CONFIG_BEAN, uiConfigSupport);
    }

    /**
     * text provider setter
     *
     * @param textProvider text provider
     */
    public void setTextProvider(com.opensymphony.xwork2.TextProvider textProvider)
    {

        this.textProvider = textProvider;
    }

    /**
     * UI Config setter
     *
     * @param uiConfigSupport ui config
     */
    public void setUiConfigSupport(UIConfigSupport uiConfigSupport)
    {

        this.uiConfigSupport = uiConfigSupport;
    }
}