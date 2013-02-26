package com.joelj.jenkins.eztemplates;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.LinkedList;
import java.util.List;

/**
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 10:11 PM
 */
public class TemplateProperty extends JobProperty<AbstractProject<?,?>> {
	private final List<String> implementations;

	@DataBoundConstructor
	public TemplateProperty(List<String> implementations) {
		this.implementations = implementations;
	}

	public List<String> getImplementations() {
		return implementations;
	}

	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {
		@Override
		public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
			if(formData.size() > 0) {
				List<String> implementationJobs = new LinkedList<String>();
				Ancestor ancestor = req.getAncestors().get(req.getAncestors().size() - 1);
				while(ancestor != null && !(ancestor.getObject() instanceof AbstractProject)) {
					ancestor = ancestor.getPrev();
				}

				if(ancestor != null) {
					AbstractProject thisProject = (AbstractProject) ancestor.getObject();

					@SuppressWarnings("unchecked")
					TemplateProperty property = (TemplateProperty) thisProject.getProperty(TemplateProperty.class);

					if(property != null) {
						List<String> oldImplementationList = property.getImplementations();
						implementationJobs.addAll(oldImplementationList);
					}
				}

				return new TemplateProperty(implementationJobs);
			}
			return null;
		}

		@Override
		public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
			return super.configure(req, json);
		}

		@Override
		public String getDisplayName() {
			return Messages.TemplateImplementationProperty_displayName();
		}
	}
}
