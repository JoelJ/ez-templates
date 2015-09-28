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

package com.joelj.jenkins.eztemplates.InheritenceStep.singlestep;

import hudson.model.Descriptor;
import hudson.model.FreeStyleProject;
import hudson.tasks.BuildStep;
import hudson.tasks.Builder;
import hudson.util.DescribableList;
import org.jenkins_ci.plugins.run_condition.BuildStepRunner;
import org.jenkins_ci.plugins.run_condition.core.AlwaysRun;
import com.joelj.jenkins.eztemplates.InheritenceStep.lister.DefaultBuilderDescriptorLister;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for the script console
 */
public class JobUpdater {

    /**
     * Wrap all of the allowed BuildSteps on a freestyle project with a single conditional builder
     * 
     * For freestyle project called 'xxx':
     * 
     * <code>
     * import static org.jenkinsci.plugins.conditionalbuildstep.singlestep.JobUpdater.*
     * 
     * def job = hudson.model.Hudson.instance.getItem('xxx')
     * updateBuilders job
     * </code>
     * 
     * Once executed, go to the configure page to check that everything looks OK, then save the configuration
     */
    public static boolean updateBuilders(final FreeStyleProject project) throws IOException {
        if (project == null) return false;
        final DescribableList<Builder, Descriptor<Builder>> builders = project.getBuildersList();
        final DefaultBuilderDescriptorLister builderLister = new DefaultBuilderDescriptorLister();
        final List<? extends Descriptor<? extends BuildStep>> allowed = builderLister.getAllowedBuilders(project);
        final List<Builder> replace = new ArrayList<Builder>();
        for (Builder builder : builders) {
            if (allowed.contains(builder.getDescriptor()))
                replace.add(new SingleConditionalBuilder((BuildStep) builder, new AlwaysRun(), new BuildStepRunner.Fail()));
            else
                replace.add(builder);
        }
        builders.replaceBy(replace);
        return true;
    }

}
