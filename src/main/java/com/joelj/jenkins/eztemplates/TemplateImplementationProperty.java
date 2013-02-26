package com.joelj.jenkins.eztemplates;

import com.joelj.jenkins.eztemplates.utils.ProjectUtils;
import hudson.Extension;
import hudson.model.*;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;

import java.util.List;

/**
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 10:11 PM
 */
public class TemplateImplementationProperty extends JobProperty<AbstractProject<?,?>> {
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
		public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			if(formData.size() > 0 && formData.has("useTemplate")) {
				return new TemplateImplementationProperty(formData.getJSONObject("useTemplate").getString("templateJobName"));
			}
			return null;
		}

		@Override
		public String getDisplayName() {
			return Messages.TemplateImplementationProperty_displayName();
		}
	}
}
