package com.joelj.jenkins.eztemplates;

import com.google.common.base.Throwables;
import com.joelj.jenkins.eztemplates.utils.TemplateUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

/**
 * React to changes being made on template implementation projects
 */
@Extension
public class TemplateImplementationProjectListener extends ItemListener {

    @Override
    public void onUpdated(Item item) {
        TemplateImplementationProperty property = getTemplateImplementationProperty(item);
        if (property != null) {
            try {
                TemplateUtils.handleTemplateImplementationSaved((AbstractProject) item, property);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    /**
     * @param item A changed project
     * @return null if this is not a template implementation project
     */
    private static TemplateImplementationProperty getTemplateImplementationProperty(Item item) {
        if (item instanceof AbstractProject) {
            return (TemplateImplementationProperty) ((AbstractProject) item).getProperty(TemplateImplementationProperty.class);
        }
        return null;
    }

}
