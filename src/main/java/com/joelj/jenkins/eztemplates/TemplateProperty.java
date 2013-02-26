package com.joelj.jenkins.eztemplates;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 10:11 PM
 */
public class TemplateProperty extends JobProperty<AbstractProject<?,?>> {
	@DataBoundConstructor
	public TemplateProperty() {
	}

	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {
		@Override
		public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			if(formData.size() > 0) {
				return new TemplateProperty();
			}
			return null;
		}

		@Override
		public String getDisplayName() {
			return Messages.TemplateImplementationProperty_displayName();
		}
	}
}
