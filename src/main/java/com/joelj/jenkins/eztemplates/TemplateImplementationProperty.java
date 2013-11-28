package com.joelj.jenkins.eztemplates;

import com.joelj.jenkins.eztemplates.utils.ProjectUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 10:11 PM
 */
public class TemplateImplementationProperty extends JobProperty<AbstractProject<?,?>> {
	private static final Logger LOG = Logger.getLogger("ez-templates");

	private String templateJobName;
    private final boolean syncMatrixAxis;
	private final boolean syncDescription;
	private final boolean syncBuildTriggers;
	private final boolean syncDisabled;

	@DataBoundConstructor
	public TemplateImplementationProperty(String templateJobName, boolean syncMatrixAxis, boolean syncDescription, boolean syncBuildTriggers, boolean syncDisabled) {
		this.templateJobName = templateJobName;
		this.syncMatrixAxis = syncMatrixAxis;
		this.syncDescription = syncDescription;
		this.syncBuildTriggers = syncBuildTriggers;
		this.syncDisabled = syncDisabled;
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

	public AbstractProject findProject() {
		return ProjectUtils.findProject(getTemplateJobName());
	}

	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {
		@Override
		public JobProperty<?> newInstance(StaplerRequest request, JSONObject formData) throws FormException {
			AbstractProject implementationProject = ProjectUtils.findProject(request);

            TemplateImplementationProperty oldProperty = (TemplateImplementationProperty)implementationProject.getProperty(TemplateImplementationProperty.class);

            boolean hasNewProperty = formData.size() > 0 && formData.has("useTemplate");
            boolean hasOldProperty = oldProperty != null;

            if(hasOldProperty) {
                boolean removeOldProperty = true;
                if (hasNewProperty) {
                    AbstractProject oldTemplate = oldProperty.findProject();
                    String templateJobName = formData.getJSONObject("useTemplate").getString("templateJobName");
                    boolean namesMatch = oldTemplate!=null && StringUtils.defaultString(templateJobName).equals(oldTemplate.getName());
                    if (namesMatch) {
                        removeOldProperty = false;
                    }
                }
                if (removeOldProperty) {
                    removeImplementationFromTemplate(oldProperty.findProject(), implementationProject);
                }
            }

            if(hasNewProperty) {
                JSONObject useTemplate = formData.getJSONObject("useTemplate");

                String templateJobName = useTemplate.getString("templateJobName");
                boolean syncMatrixAxis = useTemplate.getBoolean("syncMatrixAxis");
                boolean syncDescription = useTemplate.getBoolean("syncDescription");
                boolean syncBuildTriggers = useTemplate.getBoolean("syncBuildTriggers");
                boolean syncDisabled = useTemplate.getBoolean("syncDisabled");

                assignImplementationToTemplate(ProjectUtils.findProject(templateJobName), implementationProject);

                return new TemplateImplementationProperty(templateJobName, syncMatrixAxis, syncDescription, syncBuildTriggers, syncDisabled);
			}

			return null;
		}

        private static void removeImplementationFromTemplate(AbstractProject templateProject, AbstractProject implementationProject) throws FormException {
            if ( templateProject==null ) {
                // Could just be an empty name!
                LOG.warning(String.format("Cannot remove %s from missing template", implementationProject.getDisplayName()));
                return;
            }
            @SuppressWarnings("unchecked")
            TemplateProperty property = (TemplateProperty) templateProject.getProperty(TemplateProperty.class);

            if(property != null && property.removeImplementation(implementationProject.getName())) {
                LOG.info(String.format("Removing %s from template %s",implementationProject.getDisplayName(),templateProject.getDisplayName()));
                saveTemplate(templateProject);
            }
        }

        private static void assignImplementationToTemplate(AbstractProject templateProject, AbstractProject implementationProject) throws FormException {
            if(templateProject == null) {
                // May not have configured it yet
                return;
            }
            @SuppressWarnings("unchecked")
            TemplateProperty property = (TemplateProperty) templateProject.getProperty(TemplateProperty.class);

            if(property != null && property.addImplementation(implementationProject.getName())) {
                LOG.info(String.format("Assigning %s to template %s ",implementationProject.getDisplayName(),templateProject.getDisplayName()));
                // We did add a new implementation to the template (if it already used that project addImplementation returns false)
                saveTemplate(templateProject);
            }
        }

        private static void saveTemplate( AbstractProject templateProject ) throws FormException {
            try {
                ProjectUtils.silentSave(templateProject);
            } catch (IOException e) {
                throw new FormException(e, "templateJobName");
            }
        }

        @Override
		public String getDisplayName() {
			return Messages.TemplateImplementationProperty_displayName();
		}

        @SuppressWarnings({ "static-method", "unused" })
        public FormValidation doCheckTemplateJobName(@QueryParameter final String value) {
            if ( StringUtils.isBlank(value) ) {
                return FormValidation.warning("Template name is blank");
            }
            if ( ProjectUtils.findProject(value)==null ) {
                return FormValidation.error("Template %s not found",value);
            }
            return FormValidation.ok();
        }
	}
}
