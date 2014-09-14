package com.joelj.jenkins.eztemplates;

import com.google.common.base.Throwables;
import com.joelj.jenkins.eztemplates.utils.TemplateUtils;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;

import javax.inject.Inject;

/**
 * React to changes being made on template implementation projects
 */
@Extension
public class TemplateImplementationProjectListener extends ItemListener {

    @Inject
    private TemplateUtils templateUtils;

    @Override
    public void onUpdated(Item item) {
        TemplateImplementationProperty property = TemplateImplementationProperty.from(item);
        if (property != null) {
            try {
                templateUtils.handleTemplateImplementationSaved((AbstractProject) item, property);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

}
