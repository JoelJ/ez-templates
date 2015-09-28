package com.joelj.jenkins.eztemplates.InheritenceStep;

import hudson.maven.MavenModuleSet;
import hudson.model.AbstractProject;
import hudson.model.Project;
import hudson.tasks.BuildStep;
import jenkins.model.Jenkins;
import com.joelj.jenkins.eztemplates.InheritenceStep.singlestep.SingleConditionalBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to work with {@link BuildStep}s wrapped by {@link ConditionalBuilder} or {@link SingleConditionalBuilder}.
 * 
 * @author Dominik Bartholdi (imod)
 * 
 */
public class ConditionalBuildStepHelper {

    private ConditionalBuildStepHelper() {
    }

    /**
     * Gets the list of all buildsteps wrapped within any {@link ConditionalBuilder} or {@link SingleConditionalBuilder} from within the given project. Keeps the API backward compatible (Internally
     * calls {@link #getConditionalBuildersFromMavenProject(AbstractProject)})
     * 
     * @see https://issues.jenkins-ci.org/browse/JENKINS-20543
     * @param p
     *            the project to get all wrapped builders for
     * @param type
     *            the type of builders to search for
     * @return a list of all buildsteps, never <code>null</code>
     */
    public static <T extends BuildStep> List<T> getContainedBuilders(Project<?, ?> p, Class<T> type) {
        return getContainedBuilders((AbstractProject<?, ?>) p, type);
    }

    /**
     * Gets the list of all buildsteps wrapped within any {@link ConditionalBuilder} or {@link SingleConditionalBuilder} from within the given project.
     * 
     * @param p
     *            the project to get all wrapped builders for
     * @param type
     *            the type of builders to search for
     * @return a list of all buildsteps, never <code>null</code>
     */
    public static <T extends BuildStep> List<T> getContainedBuilders(AbstractProject<?, ?> ap, Class<T> type) {

        final boolean mavenIsInstalled = isMavenPluginInstalled();

        List<T> r = new ArrayList<T>();

        List<ConditionalBuilder> cbuilders = new ArrayList<ConditionalBuilder>();
        List<SingleConditionalBuilder> scbuilders = new ArrayList<SingleConditionalBuilder>();
        if (Project.class.isAssignableFrom(ap.getClass())) {
            Project<?, ?> p = (Project<?, ?>) ap;
            cbuilders.addAll(p.getBuildersList().getAll(ConditionalBuilder.class));
            scbuilders.addAll(p.getBuildersList().getAll(SingleConditionalBuilder.class));
        } else if (mavenIsInstalled) {
            cbuilders.addAll(getConditionalBuildersFromMavenProject(ap));
            scbuilders.addAll(getSingleConditionalBuildersFromMavenProject(ap));
        }

        for (ConditionalBuilder conditionalBuilder : cbuilders) {
            final List<BuildStep> cbs = conditionalBuilder.getConditionalbuilders();
            if (cbs != null) {
                for (BuildStep buildStep : cbs) {
                    if (type.isInstance(buildStep)) {
                        r.add(type.cast(buildStep));
                    }
                }
            }
        }

        for (SingleConditionalBuilder singleConditionalBuilder : scbuilders) {
            BuildStep buildStep = singleConditionalBuilder.getBuildStep();
            if (buildStep != null && type.isInstance(buildStep)) {
                r.add(type.cast(buildStep));
            }
        }

        return r;
    }

    private static List<ConditionalBuilder> getConditionalBuildersFromMavenProject(AbstractProject<?, ?> ap) {
        List<ConditionalBuilder> r = new ArrayList<ConditionalBuilder>();
        if (MavenModuleSet.class.isAssignableFrom(ap.getClass())) {
            MavenModuleSet ms = (MavenModuleSet) ap;
            r.addAll(ms.getPostbuilders().getAll(ConditionalBuilder.class));
            r.addAll(ms.getPrebuilders().getAll(ConditionalBuilder.class));
        }
        return r;
    }

    private static List<SingleConditionalBuilder> getSingleConditionalBuildersFromMavenProject(AbstractProject<?, ?> ap) {
        List<SingleConditionalBuilder> r = new ArrayList<SingleConditionalBuilder>();
        if (MavenModuleSet.class.isAssignableFrom(ap.getClass())) {
            MavenModuleSet ms = (MavenModuleSet) ap;
            r.addAll(ms.getPostbuilders().getAll(SingleConditionalBuilder.class));
            r.addAll(ms.getPrebuilders().getAll(SingleConditionalBuilder.class));
        }
        return r;
    }

    /**
     * Is the maven plugin installed and active?
     * 
     * @return
     */
    public static boolean isMavenPluginInstalled() {
        final hudson.Plugin plugin = Jenkins.getInstance().getPlugin("maven-plugin");
        return plugin != null ? plugin.getWrapper().isActive() : false;
    }
}
