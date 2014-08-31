package com.joelj.jenkins.eztemplates;

import com.google.common.base.Throwables;
import com.joelj.jenkins.eztemplates.utils.TemplateUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import org.apache.tools.ant.util.StringUtils;

/**
 * React to changes being made on template projects
 */
@Extension
public class TemplateProjectListener extends ItemListener {

    @Override
    public void onUpdated(Item item) {
        TemplateProperty property = getTemplateProperty(item);
        if (property != null) {
            try {
                TemplateUtils.handleTemplateSaved((AbstractProject) item, property);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public void onDeleted(Item item) {
        TemplateProperty property = getTemplateProperty(item);
        if (property != null) {
            try {
                TemplateUtils.handleTemplateDeleted((AbstractProject) item, property);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    @Override
    public void onLocationChanged(Item item, String oldFullName, String newFullName) {
        TemplateProperty property = getTemplateProperty(item);
        if (property != null) {
            try {
                TemplateUtils.handleTemplateRename((AbstractProject) item, property, oldFullName, newFullName);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    /**
     * @param item A changed project
     * @return null if this is not a template project
     */
    private static TemplateProperty getTemplateProperty(Item item) {
        if (item instanceof AbstractProject) {
            return (TemplateProperty) ((AbstractProject) item).getProperty(TemplateProperty.class);
        }
        return null;
    }

}
