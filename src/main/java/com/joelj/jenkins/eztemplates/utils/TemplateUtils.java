package com.joelj.jenkins.eztemplates.utils;

import com.joelj.jenkins.eztemplates.TemplateImplementationProperty;
import com.joelj.jenkins.eztemplates.TemplateProperty;
import hudson.model.*;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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

	public static void handleTemplate(AbstractProject templateProject, TemplateProperty property) throws IOException {
		LOG.info("Template " + templateProject.getDisplayName() + " was saved. Syncing implementations. " + property);
		Set<String> implementations = property.getImplementations();

		Iterator<String> iterator = implementations.iterator();
		boolean changedTemplateProject = false;
		while (iterator.hasNext()) {
			String implementationName = iterator.next();
			AbstractProject project = ProjectUtils.findProject(implementationName);
			if (project == null) {
				changedTemplateProject = true;
				iterator.remove();
				continue;
			}

			@SuppressWarnings("unchecked")
			TemplateImplementationProperty impProperty = (TemplateImplementationProperty) project.getProperty(TemplateImplementationProperty.class);
			handleImplementation(project, impProperty);
		}

		if(changedTemplateProject) {
			ProjectUtils.silentSave(templateProject);
		}
	}

	public static void handleImplementation(AbstractProject implementationProject, TemplateImplementationProperty property) throws IOException {
		LOG.info("Implementation " + implementationProject.getDisplayName() + " was saved. Syncing with " + property.getTemplateJobName());
		AbstractProject templateProject = property.findProject();

		@SuppressWarnings("unchecked")
		boolean implementationIsTemplate = implementationProject.getProperty(TemplateProperty.class) != null;
		List<ParameterDefinition> oldImplementationParameters = findParameters(implementationProject);

		implementationProject = synchronizeConfigFiles(implementationProject, templateProject);

		fixProperties(implementationProject, property, implementationIsTemplate);
		fixParameters(implementationProject, oldImplementationParameters);

		ProjectUtils.silentSave(implementationProject);
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
					result.add(oldImplementationParameter);
				}
			}
			if(!found) {
				//Add new parameters not accounted for.
				result.add(newImplementationParameter);
			}
		}

		if(LOG.isLoggable(Level.INFO) && oldImplementationParameters != null && oldImplementationParameters.size() > 0) {
			LOG.info("Throwing away parameters: ");
			for (ParameterDefinition newImplementationParameter : oldImplementationParameters) {
				LOG.info("\t"+newImplementationParameter.toString());
			}
		}

		return new ParametersDefinitionProperty(result);
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
		if(parametersDefinitionProperty != null) {
			for (String parameterName : parametersDefinitionProperty.getParameterDefinitionNames()) {
				definitions.add(parametersDefinitionProperty.getParameterDefinition(parameterName));
			}
		}
		return definitions;
	}

	private static void fixProperties(AbstractProject implementationProject, TemplateImplementationProperty property, boolean implementationIsTemplate) throws IOException {
		//noinspection unchecked
		implementationProject.addProperty(property);

		if (!implementationIsTemplate) {
			//noinspection unchecked
			implementationProject.removeProperty(TemplateProperty.class);
		}
	}
}
