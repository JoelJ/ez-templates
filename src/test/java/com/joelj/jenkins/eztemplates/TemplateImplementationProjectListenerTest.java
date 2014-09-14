package com.joelj.jenkins.eztemplates;

import com.joelj.jenkins.eztemplates.utils.TemplateUtils;
import hudson.model.AbstractProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TemplateImplementationProjectListenerTest {

    @Mock
    private TemplateUtils templateUtils;
    @Mock
    private AbstractProject project;
    @Mock
    private TemplateImplementationProperty property;

    @InjectMocks
    private TemplateImplementationProjectListener listener = new TemplateImplementationProjectListener();

    @Test
    public void onUpdated_ignores_changes_to_non_ezt_impls() {
        // When:
        listener.onUpdated(project);
        // Then:
        verifyNoMoreInteractions(templateUtils);
    }

    @Test
    public void onUpdated_delegates_changes_to_ezt_impls_to_handler() throws IOException {
        // Given:
        when(project.getProperty(TemplateImplementationProperty.class)).thenReturn(property);
        // When:
        listener.onUpdated(project);
        // Then:
        verify(templateUtils).handleTemplateImplementationSaved(project, property);
    }
}