/*
 * The MIT License
 *
 * Copyright (C) 2011 by Anthony Robinson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.joelj.jenkins.eztemplates.InheritenceStep.lister;

import com.joelj.jenkins.eztemplates.InheritenceStep.BuilderChain;
import com.joelj.jenkins.eztemplates.InheritenceStep.ConditionalBuilder;
import com.joelj.jenkins.eztemplates.InheritenceStep.singlestep.SingleConditionalBuilder;
import com.joelj.jenkins.eztemplates.Messages;
import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundConstructor;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public class DefaultBuilderDescriptorLister implements BuilderDescriptorLister {

    @DataBoundConstructor
    public DefaultBuilderDescriptorLister() {
    }

    public List<? extends Descriptor<? extends BuildStep>> getAllowedBuilders(final AbstractProject<?, ?> project) {
        final List<BuildStepDescriptor<? extends Builder>> builders = new ArrayList<BuildStepDescriptor<? extends Builder>>();
        if (project == null)
            return builders;
        for (Descriptor<Builder> descriptor : Builder.all()) {
            if (descriptor instanceof SingleConditionalBuilder.SingleConditionalBuilderDescriptor) {
                continue;
            }
            if (descriptor instanceof ConditionalBuilder.DescriptorImpl) {
                continue;
            }
            if (descriptor instanceof BuilderChain.DescriptorImpl) {
                continue;
            }
            if (!(descriptor instanceof BuildStepDescriptor)) {
                continue;
            }
            BuildStepDescriptor buildStepDescriptor = (BuildStepDescriptor) descriptor;
            if (buildStepDescriptor.isApplicable(project.getClass()) && hasDbc(buildStepDescriptor.clazz)) {
                builders.add(buildStepDescriptor);
            }
        }
        return builders;
    }

    public DescriptorImpl getDescriptor() {
        return Jenkins.getInstance().getDescriptorByType(DescriptorImpl.class);
    }

    private boolean hasDbc(final Class<?> clazz) {
        for (Constructor<?> constructor : clazz.getConstructors()) {
            if (constructor.isAnnotationPresent(DataBoundConstructor.class))
                return true;
        }
        return false;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<BuilderDescriptorLister> {

        @Override
        public String getDisplayName() {
            return Messages.defaultBuilderDescriptor_displayName();
        }
    }
}