package com.joelj.jenkins.eztemplates;

import com.joelj.jenkins.eztemplates.utils.ProjectUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.util.logging.Logger;

public class TemplateImplementationProperty extends JobProperty<AbstractProject<?, ?>> {
    private static final Logger LOG = Logger.getLogger("ez-templates");

    private String templateJobName;
    private final boolean syncMatrixAxis;
    private final boolean syncDescription;
    private final boolean syncBuildTriggers;
    private final boolean syncDisabled;
    private final boolean syncSecurity;
    private final boolean syncScm;

    @DataBoundConstructor
    public TemplateImplementationProperty(String templateJobName, boolean syncMatrixAxis, boolean syncDescription, boolean syncBuildTriggers, boolean syncDisabled, boolean syncSecurity, boolean syncScm) {
        this.templateJobName = templateJobName;
        this.syncMatrixAxis = syncMatrixAxis;
        this.syncDescription = syncDescription;
        this.syncBuildTriggers = syncBuildTriggers;
        this.syncDisabled = syncDisabled;
        this.syncSecurity = syncSecurity;
        this.syncScm = syncScm;
    }

    @Exported
    public String getTemplateJobName() {
        return templateJobName;
    }

    public void setTemplateJobName(String templateJobName) {
        this.templateJobName = templateJobName;
    }

    @Exported
    public boolean getSyncMatrixAxis() {
        return syncMatrixAxis;
    }

    public boolean getSyncDescription() {
        return syncDescription;
    }

    public boolean getSyncBuildTriggers() {
        return syncBuildTriggers;
    }

    public boolean getSyncDisabled() {
        return syncDisabled;
    }

    public boolean getSyncSecurity() {
        return syncSecurity;
    }

    public boolean getSyncScm() {
        return syncScm;
    }

    public AbstractProject findTemplate() {
        return ProjectUtils.findProject(getTemplateJobName());
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {
        @Override
        public JobProperty<?> newInstance(StaplerRequest request, JSONObject formData) throws FormException {
            if (formData.size() > 0 && formData.has("useTemplate")) {
                JSONObject useTemplate = formData.getJSONObject("useTemplate");

                String templateJobName = useTemplate.getString("templateJobName");
                boolean syncMatrixAxis = useTemplate.getBoolean("syncMatrixAxis");
                boolean syncDescription = useTemplate.getBoolean("syncDescription");
                boolean syncBuildTriggers = useTemplate.getBoolean("syncBuildTriggers");
                boolean syncDisabled = useTemplate.getBoolean("syncDisabled");
                boolean syncSecurity = useTemplate.getBoolean("syncSecurity");
                boolean syncScm = useTemplate.getBoolean("syncScm");

                return new TemplateImplementationProperty(templateJobName, syncMatrixAxis, syncDescription, syncBuildTriggers, syncDisabled, syncSecurity, syncScm);
            }

            return null;
        }

        /**
         * Jenkins-convention to populate the drop-down box with discovered templates
         */
        @SuppressWarnings("UnusedDeclaration")
        public ListBoxModel doFillTemplateJobNameItems() {
            ListBoxModel items = new ListBoxModel();
            // Add null as first option - dangerous to force an existing project onto a template in case
            // a noob destroys their config
            items.add(Messages.TemplateImplementationProperty_noTemplateSelected(), null);
            // Add all discovered templates
            for (AbstractProject project : ProjectUtils.findProjectsWithProperty(TemplateProperty.class)) {
                // fullName includes any folder structure
                items.add(project.getFullDisplayName(), project.getFullName());
            }
            return items;
        }

        @Override
        public String getDisplayName() {
            return Messages.TemplateImplementationProperty_displayName();
        }

        @SuppressWarnings({"static-method", "unused"})
        public FormValidation doCheckTemplateJobName(@QueryParameter final String value) {
            if (StringUtils.isBlank(value)) {
                return FormValidation.error(Messages.TemplateImplementationProperty_noTemplateSelected());
            }
            return FormValidation.ok();
        }
    }
}
