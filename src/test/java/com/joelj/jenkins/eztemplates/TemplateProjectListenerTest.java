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
public class TemplateProjectListenerTest {

    @Mock
    private TemplateUtils templateUtils;
    @Mock
    private AbstractProject project;
    @Mock
    private TemplateProperty property;

    @InjectMocks
    private TemplateProjectListener listener = new TemplateProjectListener();

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
        when(project.getProperty(TemplateProperty.class)).thenReturn(property);
        // When:
        listener.onUpdated(project);
        // Then:
        verify(templateUtils).handleTemplateSaved(project, property);
    }

    @Test
    public void onDeleted_ignores_changes_to_non_ezt_impls() {
        // When:
        listener.onDeleted(project);
        // Then:
        verifyNoMoreInteractions(templateUtils);
    }

    @Test
    public void onDeleted_delegates_changes_to_ezt_impls_to_handler() throws IOException {
        // Given:
        when(project.getProperty(TemplateProperty.class)).thenReturn(property);
        // When:
        listener.onDeleted(project);
        // Then:
        verify(templateUtils).handleTemplateDeleted(project, property);
    }

    @Test
    public void onLocationChanged_ignores_changes_to_non_ezt_impls() {
        // When:
        listener.onLocationChanged(project, "old", "new");
        // Then:
        verifyNoMoreInteractions(templateUtils);
    }

    @Test
    public void onLocationChanged_delegates_changes_to_ezt_impls_to_handler() throws IOException {
        // Given:
        when(project.getProperty(TemplateProperty.class)).thenReturn(property);
        // When:
        listener.onLocationChanged(project, "old", "new");
        // Then:
        verify(templateUtils).handleTemplateRename(project, property, "old", "new");
    }
}