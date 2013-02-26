package com.joelj.jenkins.eztemplates;

import com.joelj.jenkins.eztemplates.utils.ProjectUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 10:11 PM
 */
public class TemplateProperty extends JobProperty<AbstractProject<?,?>> {
	private final Set<String> implementations;

	@DataBoundConstructor
	public TemplateProperty(Set<String> implementations) {
		this.implementations = implementations;
	}

	public Set<String> getImplementations() {
		return implementations;
	}

	public void addImplementation(String projectName) {
		implementations.add(projectName);
	}

	public void removeImplementation(String projectName) {
		implementations.remove(projectName);
	}

	@Extension
	public static class DescriptorImpl extends JobPropertyDescriptor {
		@Override
		public JobProperty<?> newInstance(StaplerRequest request, JSONObject formData) throws FormException {
			if(formData.size() > 0) {
				Set<String> implementationJobs = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

				AbstractProject thisProject = ProjectUtils.findProject(request);
				if(thisProject != null) {
					@SuppressWarnings("unchecked")
					TemplateProperty property = (TemplateProperty) thisProject.getProperty(TemplateProperty.class);

					if(property != null) {
						Set<String> oldImplementationList = property.getImplementations();
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
