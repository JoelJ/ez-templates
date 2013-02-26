package com.joelj.jenkins.eztemplates;

import com.joelj.jenkins.eztemplates.utils.ProjectUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 10:11 PM
 */
public class TemplateImplementationProperty extends JobProperty<AbstractProject<?,?>> {
	private static final Logger LOG = Logger.getLogger("ez-templates");

	private final String templateJobName;

	@DataBoundConstructor
	public TemplateImplementationProperty(String templateJobName) {
		this.templateJobName = templateJobName;
	}

	@Exported
	public String getTemplateJobName() {
		return templateJobName;
	}

	public AbstractProject findProject() {
		return ProjectUtils.findProject(getTemplateJobName());
	}

	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {
		@Override
		public JobProperty<?> newInstance(StaplerRequest request, JSONObject formData) throws FormException {
			AbstractProject thisProject = ProjectUtils.findProject(request);
			String thisProjectName = thisProject.getName();

			if(formData.size() > 0 && formData.has("useTemplate")) {
				String templateJobName = formData.getJSONObject("useTemplate").getString("templateJobName");
				AbstractProject templateJob = ProjectUtils.findProject(templateJobName);
				if(templateJob != null) {
					@SuppressWarnings("unchecked")
					TemplateProperty property = (TemplateProperty) templateJob.getProperty(TemplateProperty.class);
					property.addImplementation(thisProjectName);

					try {
						ProjectUtils.silentSave(templateJob);
					} catch (IOException e) {
						throw new FormException(e, "templateJobName");
					}
				}
				return new TemplateImplementationProperty(templateJobName);
			} else {
				@SuppressWarnings("unchecked")
				TemplateImplementationProperty oldTemplateImplementationProperty = (TemplateImplementationProperty) thisProject.getProperty(TemplateImplementationProperty.class);
				if(oldTemplateImplementationProperty != null) {
					LOG.info(thisProjectName + "No longer implementing template: " + oldTemplateImplementationProperty.getTemplateJobName());
					//TemplateImplementationProperty was just removed. So notify the template.
					AbstractProject templateJob = oldTemplateImplementationProperty.findProject();
					if(templateJob != null) {
						@SuppressWarnings("unchecked")
						TemplateProperty property = (TemplateProperty) templateJob.getProperty(TemplateProperty.class);
						property.removeImplementation(thisProjectName);

						try {
							ProjectUtils.silentSave(templateJob);
						} catch (IOException e) {
							throw new FormException(e, "templateJobName");
						}
					} else {
						LOG.warning(thisProjectName + " used to implement template " + oldTemplateImplementationProperty.getTemplateJobName() + " but that project cannot be found so we can't unregister this implementation.");
					}
				}
			}
			return null;
		}

		@Override
		public String getDisplayName() {
			return Messages.TemplateImplementationProperty_displayName();
		}
	}
}
