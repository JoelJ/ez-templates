package com.joelj.jenkins.eztemplates;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.joelj.jenkins.eztemplates.utils.ProjectUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.JobProperty;
import hudson.model.JobPropertyDescriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Collection;

public class TemplateProperty extends JobProperty<AbstractProject<?, ?>> {

    /**
     * @return null if this is not a template implementation project
     */
    public static TemplateProperty from(Item item) {
        if (item instanceof AbstractProject) {
            return from((AbstractProject) item);
        }
        return null;
    }

    /**
     * @return null if this is not a template implementation project
     */
    public static TemplateProperty from(@Nonnull AbstractProject project) {
        return (TemplateProperty) project.getProperty(TemplateProperty.class);
    }

    @Inject
    private ProjectUtils projectUtils;

    // TODO should probably not be implemented here
    public Collection<AbstractProject> getImplementations(final String templateFullName) {
        Collection<AbstractProject> projects = projectUtils.findProjectsWithProperty(TemplateImplementationProperty.class);
        return Collections2.filter(projects, new Predicate<AbstractProject>() {
            @Override
            public boolean apply(AbstractProject abstractProject) {
                TemplateImplementationProperty prop = (TemplateImplementationProperty) abstractProject.getProperty(TemplateImplementationProperty.class);
                return templateFullName.equals(prop.getTemplateJobName());
            }
        });

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
