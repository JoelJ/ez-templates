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
import hudson.security.*;
import hudson.scm.SCM;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class TemplateUtils {
    private static final Logger LOG = Logger.getLogger("ez-templates");

    public static void handleTemplateSaved(AbstractProject templateProject, TemplateProperty property) throws IOException {
        LOG.info(String.format("Template [%s] was saved. Syncing implementations.", templateProject.getFullDisplayName()));
        for (AbstractProject impl : property.getImplementations()) {
            TemplateImplementationProperty implProperty = (TemplateImplementationProperty) impl.getProperty(TemplateImplementationProperty.class);
            handleTemplateImplementationSaved(impl, implProperty);
        }
    }

    public static void handleTemplateDeleted(AbstractProject templateProject, TemplateProperty property) throws IOException {
        LOG.info(String.format("Template [%s] was deleted.", templateProject.getFullDisplayName()));
        for (AbstractProject impl : property.getImplementations()) {
            LOG.info(String.format("Removing template from [%s].", impl.getFullDisplayName()));
            TemplateImplementationProperty implProperty = (TemplateImplementationProperty) impl.getProperty(TemplateImplementationProperty.class);
            impl.removeProperty(TemplateImplementationProperty.class);
            ProjectUtils.silentSave(impl);
        }
    }

    public static void handleTemplateRename(AbstractProject templateProject, TemplateProperty property, String oldFullName, String newFullName) throws IOException {
        LOG.info(String.format("Template [%s] was renamed. Updating implementations.", templateProject.getFullDisplayName()));
        for (AbstractProject impl : TemplateProperty.getImplementations(oldFullName)) {
            LOG.info(String.format("Updating template in [%s].", impl.getFullDisplayName()));
            TemplateImplementationProperty implProperty = (TemplateImplementationProperty) impl.getProperty(TemplateImplementationProperty.class);
            if (oldFullName.equals(implProperty.getTemplateJobName())) {
                implProperty.setTemplateJobName(newFullName);
                ProjectUtils.silentSave(impl);
            }
        }
    }

    public static void handleTemplateCopied(AbstractProject copy, AbstractProject original) throws IOException {
        LOG.info(String.format("Template [%s] was copied to [%s]. Forcing new project to be an implementation of the original.",original.getFullDisplayName(), copy.getFullDisplayName()));
        copy.removeProperty(TemplateProperty.class);
        copy.removeProperty(TemplateImplementationProperty.class);
        TemplateImplementationProperty implProperty = new TemplateImplementationProperty(
                original.getFullName(),
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );
        copy.addProperty(implProperty);
    }

    public static void handleTemplateImplementationSaved(AbstractProject implementationProject, TemplateImplementationProperty property) throws IOException {
    	
        if (property.getTemplateJobName().equals("null")) {
            LOG.warning(String.format("Implementation [%s] was saved. No template selected.", implementationProject.getFullDisplayName()));
            return;
        }
    	
        LOG.info(String.format("Implementation [%s] was saved. Syncing with [%s].", implementationProject.getFullDisplayName(), property.getTemplateJobName()));
        
        AbstractProject templateProject = property.findTemplate();        
        if (templateProject == null) {
        	
        	// If the template can't be found, then it's probably a bug
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
        AuthorizationMatrixProperty oldAuthMatrixProperty = (AuthorizationMatrixProperty) implementationProject.getProperty(AuthorizationMatrixProperty.class);
        SCM oldScm = (SCM) implementationProject.getScm();
        JobProperty oldOwnership = implementationProject.getProperty("com.synopsys.arc.jenkins.plugins.ownership.jobs.JobOwnerJobProperty");

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

        if (!property.getSyncSecurity() && oldAuthMatrixProperty != null) {
            implementationProject.removeProperty(AuthorizationMatrixProperty.class);
            implementationProject.addProperty(oldAuthMatrixProperty);
        }

        if (!property.getSyncScm() && oldScm != null) {
            implementationProject.setScm(oldScm);
        }

        if (!property.getSyncOwnership() && oldOwnership != null) {
            implementationProject.removeProperty(oldOwnership.getClass());
            implementationProject.addProperty(oldOwnership);
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
