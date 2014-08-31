package com.joelj.jenkins.eztemplates.utils;

import com.joelj.jenkins.eztemplates.TemplateImplementationProperty;
import com.joelj.jenkins.eztemplates.TemplateProperty;
import hudson.matrix.AxisList;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixProject;
import hudson.model.*;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.CopyOnWriteList;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * This is where all the magic really happens.
 * The templates and implementations, when they change, call one of the two public handle* methods.
 * <p/>
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 10:55 PM
 */
public class TemplateUtils {
    private static final Logger LOG = Logger.getLogger("ez-templates");

    public static void handleTemplateSaved(AbstractProject templateProject, TemplateProperty property) throws IOException {
        LOG.info(String.format("Template [%s] was saved. Syncing implementations.", templateProject.getFullDisplayName()));
        Set<String> implementations = property.getImplementations();

        Iterator<String> iterator = implementations.iterator();
        boolean changedTemplateProject = false;
        while (iterator.hasNext()) {
            String implementationName = iterator.next();
            AbstractProject project = ProjectUtils.findProject(implementationName);
            if (project == null) {
                LOG.warning(String.format("[%s] doesn't exist as a project. Cleaning it out of the template.", implementationName));
                changedTemplateProject = true;
                iterator.remove();
                continue;
            }

            @SuppressWarnings("unchecked")
            TemplateImplementationProperty impProperty = (TemplateImplementationProperty) project.getProperty(TemplateImplementationProperty.class);

            if (impProperty == null || !templateProject.getFullName().equals(impProperty.getTemplateJobName())) {
                LOG.warning(String.format("[%s] doesn't inherit from this template. Cleaning it out of the template.", implementationName));
                changedTemplateProject = true;
                iterator.remove();
                continue;
            }

            handleTemplateImplementationSaved(project, impProperty);
        }

        if (changedTemplateProject) {
            ProjectUtils.silentSave(templateProject);
        }
    }

    public static void handleTemplateDeleted(AbstractProject templateProject, TemplateProperty property) throws IOException {
        LOG.info(String.format("Template [%s] was deleted. Removing from implementations.", templateProject.getFullDisplayName()));
        for (String implementationName : property.getImplementations()) {
            AbstractProject implementationProject = ProjectUtils.findProject(implementationName);
            if (implementationProject != null) {
                LOG.info(String.format("Removing template from [%s].", implementationProject.getFullDisplayName()));
                implementationProject.removeProperty(TemplateImplementationProperty.class);
                ProjectUtils.silentSave(implementationProject);
            }
        }
    }

    public static void handleTemplateRename(AbstractProject templateProject, TemplateProperty property, String oldName, String newName) throws IOException {
        LOG.info(String.format("Template [%s] was renamed. Updating implementations.", templateProject.getFullDisplayName()));
        for (String implementationName : property.getImplementations()) {
            AbstractProject implementationProject = ProjectUtils.findProject(implementationName);
            if (implementationProject != null) {
                LOG.info(String.format("Updating template in [%s].", implementationProject.getFullDisplayName()));
                TemplateImplementationProperty implementationProperty = (TemplateImplementationProperty) implementationProject.getProperty(TemplateImplementationProperty.class);
                if (oldName.equals(implementationProperty.getTemplateJobName())) {
                    implementationProperty.setTemplateJobName(newName);
                    ProjectUtils.silentSave(implementationProject);
                }
            }
        }
    }

    public static void handleTemplateImplementationSaved(AbstractProject implementationProject, TemplateImplementationProperty property) throws IOException {
        LOG.info(String.format("Implementation [%s] was saved. Syncing with [%s].", implementationProject.getFullDisplayName(), property.getTemplateJobName()));
        AbstractProject templateProject = property.findProject();
        if (templateProject == null) {
            throw new IllegalStateException(String.format("Cannot find template [%s] used by job [%s]", property.getTemplateJobName(), implementationProject.getFullDisplayName()));
        }

        //Capture values we want to keep
        @SuppressWarnings("unchecked")
        boolean implementationIsTemplate = implementationProject.getProperty(TemplateProperty.class) != null;
        List<ParameterDefinition> oldImplementationParameters = findParameters(implementationProject);
        @SuppressWarnings("unchecked")
        Map<TriggerDescriptor, Trigger> oldTriggers = implementationProject.getTriggers();
        boolean shouldBeDisabled = implementationProject.isDisabled();
        String description = implementationProject.getDescription();

        AxisList oldAxisList = null;
        if (implementationProject instanceof MatrixProject && !property.getSyncMatrixAxis()) {
            MatrixProject matrixProject = (MatrixProject) implementationProject;
            oldAxisList = matrixProject.getAxes();
        }

        implementationProject = synchronizeConfigFiles(implementationProject, templateProject);

        // Reverse all the fields that we've marked as "Don't Sync" so that they appear that they haven't changed.

        //Set values that we wanted to keep via reflection to prevent infinite save recursion
        fixProperties(implementationProject, property, implementationIsTemplate);
        fixParameters(implementationProject, oldImplementationParameters);

        if (!property.getSyncBuildTriggers()) {
            fixBuildTriggers(implementationProject, oldTriggers);
        }

        if (!property.getSyncDisabled()) {
            ReflectionUtils.setFieldValue(AbstractProject.class, implementationProject, "disabled", shouldBeDisabled);
        }

        if (oldAxisList != null && implementationProject instanceof MatrixProject && !property.getSyncMatrixAxis()) {
            fixAxisList((MatrixProject) implementationProject, oldAxisList);
        }

        if (!property.getSyncDescription() && description != null) {
            ReflectionUtils.setFieldValue(AbstractItem.class, implementationProject, "description", description);
        }

        ProjectUtils.silentSave(implementationProject);
    }

    /**
     * Inlined from {@link MatrixProject#setAxes(hudson.matrix.AxisList)} except it doesn't call save.
     *
     * @param matrixProject The project to set the Axis on.
     * @param axisList      The Axis list to set.
     */
    private static void fixAxisList(MatrixProject matrixProject, AxisList axisList) {
        if (axisList == null) {
            return; //The "axes" field can never be null. So just to be extra careful.
        }
        ReflectionUtils.setFieldValue(MatrixProject.class, matrixProject, "axes", axisList);

        //noinspection unchecked
        ReflectionUtils.invokeMethod(MatrixProject.class, matrixProject, "rebuildConfigurations", ReflectionUtils.MethodParameter.get(MatrixBuild.MatrixBuildExecution.class, null));
    }

    private static void fixBuildTriggers(AbstractProject implementationProject, Map<TriggerDescriptor, Trigger> oldTriggers) {
        List<Trigger<?>> triggersToReplace = ProjectUtils.getTriggers(implementationProject);
        if (triggersToReplace == null) {
            throw new NullPointerException("triggersToReplace");
        }

        if (!triggersToReplace.isEmpty() || !oldTriggers.isEmpty()) {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (triggersToReplace) {
                triggersToReplace.clear();
                for (Trigger trigger : oldTriggers.values()) {
                    triggersToReplace.add(trigger);
                }
            }
        }
    }

    private static void fixParameters(AbstractProject implementationProject, List<ParameterDefinition> oldImplementationParameters) throws IOException {
        List<ParameterDefinition> newImplementationParameters = findParameters(implementationProject);

        ParametersDefinitionProperty newParameterAction = findParametersToKeep(oldImplementationParameters, newImplementationParameters);
        @SuppressWarnings("unchecked") ParametersDefinitionProperty toRemove = (ParametersDefinitionProperty) implementationProject.getProperty(ParametersDefinitionProperty.class);
        if (toRemove != null) {
            //noinspection unchecked
            implementationProject.removeProperty(toRemove);
        }
        if (newParameterAction != null) {
            //noinspection unchecked
            implementationProject.addProperty(newParameterAction);
        }
    }

    private static ParametersDefinitionProperty findParametersToKeep(List<ParameterDefinition> oldImplementationParameters, List<ParameterDefinition> newImplementationParameters) {
        List<ParameterDefinition> result = new LinkedList<ParameterDefinition>();
        for (ParameterDefinition newImplementationParameter : newImplementationParameters) { //'new' parameters are the same as the template.
            boolean found = false;
            Iterator<ParameterDefinition> iterator = oldImplementationParameters.iterator();
            while (iterator.hasNext()) {
                ParameterDefinition oldImplementationParameter = iterator.next();
                if (newImplementationParameter.getName().equals(oldImplementationParameter.getName())) {
                    found = true;
                    iterator.remove(); //Make the next iteration a little faster.
                    // #17 Description on parameters should always be overridden by template
                    ReflectionUtils.setFieldValue(ParameterDefinition.class, oldImplementationParameter, "description", newImplementationParameter.getDescription());
                    result.add(oldImplementationParameter);
                }
            }
            if (!found) {
                //Add new parameters not accounted for.
                result.add(newImplementationParameter);
                LOG.info(String.format("\t+++ new parameter [%s]", newImplementationParameter.getName()));
            }
        }

        if (oldImplementationParameters != null) {
            for (ParameterDefinition unused : oldImplementationParameters) {
                LOG.info(String.format("\t--- old parameter [%s]", unused.getName()));
            }
        }

        return result.isEmpty() ? null : new ParametersDefinitionProperty(result);
    }

    private static AbstractProject synchronizeConfigFiles(AbstractProject implementationProject, AbstractProject templateProject) throws IOException {
        File templateConfigFile = templateProject.getConfigFile().getFile();
        BufferedReader reader = new BufferedReader(new FileReader(templateConfigFile));
        try {
            Source source = new StreamSource(reader);
            implementationProject = ProjectUtils.updateProjectWithXmlSource(implementationProject, source);
        } finally {
            reader.close();
        }
        return implementationProject;
    }

    private static List<ParameterDefinition> findParameters(AbstractProject implementationProject) {
        List<ParameterDefinition> definitions = new LinkedList<ParameterDefinition>();
        @SuppressWarnings("unchecked")
        ParametersDefinitionProperty parametersDefinitionProperty = (ParametersDefinitionProperty) implementationProject.getProperty(ParametersDefinitionProperty.class);
        if (parametersDefinitionProperty != null) {
            for (String parameterName : parametersDefinitionProperty.getParameterDefinitionNames()) {
                definitions.add(parametersDefinitionProperty.getParameterDefinition(parameterName));
            }
        }
        return definitions;
    }

    private static void fixProperties(AbstractProject implementationProject, TemplateImplementationProperty property, boolean implementationIsTemplate) throws IOException {
        CopyOnWriteList<JobProperty<?>> properties = ReflectionUtils.getFieldValue(Job.class, implementationProject, "properties");
        properties.add(property);

        if (!implementationIsTemplate) {
            for (JobProperty<?> jobProperty : properties) {
                if (jobProperty instanceof TemplateProperty) {
                    properties.remove(jobProperty);
                }
            }
        }
    }

}
