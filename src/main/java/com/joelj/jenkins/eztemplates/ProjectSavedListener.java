package com.joelj.jenkins.eztemplates;

import com.joelj.jenkins.eztemplates.utils.TemplateUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import java.io.IOException;

/**
 * User: Joel Johnson
 * Date: 2/25/13
 * Time: 10:42 PM
 */
@Extension
public class ProjectSavedListener extends ItemListener {
	@SuppressWarnings("unchecked")
	@Override
	public void onUpdated(Item item) {
		if(item instanceof AbstractProject) {
			AbstractProject project = (AbstractProject) item;
			TemplateProperty templateProperty = (TemplateProperty) project.getProperty(TemplateProperty.class);
			if(templateProperty != null) {
				try {
					TemplateUtils.handleTemplate(project, templateProperty);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				TemplateImplementationProperty templateImplementationProperty = (TemplateImplementationProperty) project.getProperty(TemplateImplementationProperty.class);
				if(templateImplementationProperty != null) {
					try {
						TemplateUtils.handleImplementation(project, templateImplementationProperty);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
	}
}
