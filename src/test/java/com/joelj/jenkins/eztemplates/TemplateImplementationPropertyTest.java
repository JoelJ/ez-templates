package com.joelj.jenkins.eztemplates;

import hudson.model.AbstractProject;
import hudson.model.Item;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TemplateImplementationPropertyTest {

    @Mock
    private AbstractProject project;
    @Mock
    private Item notAProject;
    @Mock
    private TemplateImplementationProperty property;

    @Test
    public void returns_no_property_for_non_ezt_impls() {
        assertThat(TemplateImplementationProperty.from(project), is(nullValue()));
    }

    @Test
    public void returns_no_property_for_non_projects() {
        assertThat(TemplateImplementationProperty.from(notAProject), is(nullValue()));
    }

    @Test
    public void returns_property_for_ezt_impls() {
        // Given:
        when(project.getProperty(TemplateImplementationProperty.class)).thenReturn(property);
        // Then:
        assertThat(TemplateImplementationProperty.from(project), is(property));
    }
}