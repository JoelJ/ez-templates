package com.joelj.jenkins.eztemplates;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.joelj.jenkins.eztemplates.utils.ProjectUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Collection;

public class TemplateProperty extends JobProperty<AbstractProject<?, ?>> {

    public static Collection<AbstractProject> getImplementations(final String templateFullName) {
        Collection<AbstractProject> projects = ProjectUtils.findProjectsWithProperty(TemplateImplementationProperty.class);
        return Collections2.filter(projects, new Predicate<AbstractProject>() {
            public boolean apply(AbstractProject abstractProject) {
                TemplateImplementationProperty prop = (TemplateImplementationProperty) abstractProject.getProperty(TemplateImplementationProperty.class);
                return templateFullName.equals(prop.getTemplateJobName());
            }
        });

    }

    @DataBoundConstructor
    public TemplateProperty() {
    }

    public Collection<AbstractProject> getImplementations() {
        return getImplementations(owner.getFullName());
    }

    @Extension
    public static class DescriptorImpl extends JobPropertyDescriptor {
        @Override
        public JobProperty<?> newInstance(StaplerRequest request, JSONObject formData) throws FormException {
            if (formData.size() > 0) {
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
