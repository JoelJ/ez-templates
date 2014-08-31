package com.joelj.jenkins.eztemplates.utils;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Items;
import hudson.model.JobProperty;
import hudson.triggers.Trigger;
import hudson.util.AtomicFileWriter;
import hudson.util.IOException2;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

/**
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 11:49 PM
 */
public class ProjectUtils {

    public static Collection<AbstractProject> findProjectsWithProperty(final Class<? extends JobProperty<?>> property) {
        List<AbstractProject> projects = Jenkins.getInstance().getAllItems(AbstractProject.class);
        return Collections2.filter(projects, new Predicate<AbstractProject>() {
            @Override
            public boolean apply(AbstractProject abstractProject) {
                return abstractProject.getProperty(property) != null;
            }
        });
    }

    public static AbstractProject findProject(StaplerRequest request) {
        Ancestor ancestor = request.getAncestors().get(request.getAncestors().size() - 1);
        while (ancestor != null && !(ancestor.getObject() instanceof AbstractProject)) {
            ancestor = ancestor.getPrev();
        }
        if (ancestor == null) {
            return null;
        }
        return (AbstractProject) ancestor.getObject();
    }

    /**
     * Get a project by its fullName (including any folder structure if present).
     * Temporarily also allows a match by name if one exists.
     */
    public static AbstractProject findProject(String fullName) {
        List<AbstractProject> projects = Jenkins.getInstance().getAllItems(AbstractProject.class);
        AbstractProject nameOnlyMatch = null; // marc: 20140831, Remove compat patch for users upgrading
        for (AbstractProject project : projects) {
            if (fullName.equals(project.getFullName())) {
                return project;
            }
            if (fullName.equals(project.getName())) {
                nameOnlyMatch = project;
            }
        }
        return nameOnlyMatch;
    }

    /**
     * Silently saves the project without triggering any save events.
     * Use this method to save a project from within an Update event handler.
     */
    public static void silentSave(AbstractProject project) throws IOException {
        project.getConfigFile().write(project);
    }

    /**
     * Copied from {@link AbstractProject#updateByXml(javax.xml.transform.Source)}, removing the save event and
     * returning the project after the update.
     */
    @SuppressWarnings("unchecked")
    public static AbstractProject updateProjectWithXmlSource(AbstractProject project, Source source) throws IOException {
        String projectName = project.getName();

        XmlFile configXmlFile = project.getConfigFile();
        AtomicFileWriter out = new AtomicFileWriter(configXmlFile.getFile());
        try {
            try {
                // this allows us to use UTF-8 for storing data,
                // plus it checks any well-formedness issue in the submitted
                // data
                Transformer t = TransformerFactory.newInstance()
                        .newTransformer();
                t.transform(source,
                        new StreamResult(out));
                out.close();
            } catch (TransformerException e) {
                throw new IOException2("Failed to persist configuration.xml", e);
            }

            // try to reflect the changes by reloading
            new XmlFile(Items.XSTREAM, out.getTemporaryFile()).unmarshal(project);
            project.onLoad(project.getParent(), project.getRootDir().getName());
            Jenkins.getInstance().rebuildDependencyGraph();

            // if everything went well, commit this new version
            out.commit();
            return ProjectUtils.findProject(projectName);
        } finally {
            out.abort(); // don't leave anything behind
        }
    }

    public static List<Trigger<?>> getTriggers(AbstractProject implementationProject) {
        try {
            Field triggers = AbstractProject.class.getDeclaredField("triggers");
            triggers.setAccessible(true);
            Object result = triggers.get(implementationProject);
            //noinspection unchecked
            return (List<Trigger<?>>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
