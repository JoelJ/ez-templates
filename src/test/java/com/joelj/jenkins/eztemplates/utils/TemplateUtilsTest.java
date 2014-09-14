package com.joelj.jenkins.eztemplates.utils;

import com.google.common.collect.ImmutableList;
import com.joelj.jenkins.eztemplates.TemplateImplementationProperty;
import hudson.model.AbstractProject;
import hudson.model.JobProperty;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TemplateUtilsTest {

    @Mock
    private ProjectUtils projectUtils;

    @Mock
    private ReflectionUtils reflectionUtils;

    @InjectMocks
    private TemplateUtils templateUtils = new TemplateUtils();
    private ImmutableList<AbstractProject> allTemplateProjects;

    @Before
    public void setup() {
        TemplateImplementationProperty templ1 = new TemplateImplementationProperty("template1", false, false, false, false);
        TemplateImplementationProperty templ2 = new TemplateImplementationProperty("template2", false, false, false, false);
        AbstractProject impl1 = projectWith(templ1);
        AbstractProject impl2 = projectWith(templ1);
        AbstractProject impl3 = projectWith(templ2);
        allTemplateProjects = ImmutableList.of(impl1, impl2, impl3);
    }

    public static AbstractProject projectWith(JobProperty property) {
        AbstractProject project = mock(AbstractProject.class);
        when(project.getProperty(property.getClass())).thenReturn(property);
        return project;
    }

    @Test
    public void finds_implementations_with_matching_template() {
        // Given:
        when(projectUtils.findProjectsWithProperty(TemplateImplementationProperty.class)).thenReturn(allTemplateProjects);
        // When:
        Collection<AbstractProject> found = templateUtils.implementationsOf("template1");
        // Then:
        assertThat(found, hasSize(2));
    }

}