package com.joelj.jenkins.eztemplates.utils;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import hudson.XmlFile;
import hudson.model.AbstractProject;
import hudson.model.Items;
import hudson.model.JobProperty;
import hudson.triggers.Trigger;
import hudson.util.AtomicFileWriter;
import hudson.util.IOException2;
import jenkins.model.Jenkins;

import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.StaplerRequest;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

public class ProjectUtils {

    public static Collection<AbstractProject> findProjectsWithProperty(final Class<? extends JobProperty<?>> property) {
        List<AbstractProject> projects = Jenkins.getInstance().getAllItems(AbstractProject.class);
        return Collections2.filter(projects, new Predicate<AbstractProject>() {
            @Override
            public boolean apply(AbstractProject abstractProject) {
                return abstractProject.getProperty(property) != null;
            }
        });
    }

    public static AbstractProject findProject(StaplerRequest request) {
        Ancestor ancestor = request.getAncestors().get(request.getAncestors().size() - 1);
        while (ancestor != null && !(ancestor.getObject() instanceof AbstractProject)) {
            ancestor = ancestor.getPrev();
        }
        if (ancestor == null) {
            return null;
        }
        return (AbstractProject) ancestor.getObject();
    }

    /**
     * Get a project by its fullName (including any folder structure if present).
     * Temporarily also allows a match by name if one exists.
     */
    public static AbstractProject findProject(String fullName) {
        List<AbstractProject> projects = Jenkins.getInstance().getAllItems(AbstractProject.class);
        AbstractProject nameOnlyMatch = null; // marc: 20140831, Remove compat patch for users upgrading
        for (AbstractProject project : projects) {
            if (fullName.equals(project.getFullName())) {
                return project;
            }
            if (fullName.equals(project.getName())) {
                nameOnlyMatch = project;
            }
        }
        return nameOnlyMatch;
    }

    /**
     * Silently saves the project without triggering any save events.
     * Use this method to save a project from within an Update event handler.
     */
    public static void silentSave(AbstractProject project) throws IOException {
        project.getConfigFile().write(project);
    }

    private static Characters getNewCharactersEvent(XMLEventFactory m_eventFactory, Characters event) {
    	return m_eventFactory.createCharacters(event.getData().replaceAll("#\\{.+\\}", "TO_BE_DEFINED"));
//        if (event.getData().equalsIgnoreCase("Name1")) {
//            return m_eventFactory.createCharacters(Calendar.getInstance().getTime().toString());
//            reader.getText().replaceAll("#\\{.+\\}", "TO_BE_DEFINED");
//        } else {
//            return event;
//        }
    }         
    
    /**
     * Copied from {@link AbstractProject#updateByXml(javax.xml.transform.Source)}, removing the save event and
     * returning the project after the update.
     * @throws FactoryConfigurationError 
     * @throws XMLStreamException 
     */
    @SuppressWarnings("unchecked")
    public static AbstractProject updateProjectWithXmlSource(AbstractProject project, Source source) throws IOException {

        XmlFile configXmlFile = project.getConfigFile();
        AtomicFileWriter out = new AtomicFileWriter(configXmlFile.getFile());
        try {
        	InputStream xsltFileStream = null;
            try {         
            	
            		XMLEventFactory m_eventFactory = XMLEventFactory.newInstance();
                                                  
                    XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(source);
                    XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(out);

                    while (reader.hasNext()) {
                        XMLEvent event = (XMLEvent) reader.next();

                        if (event.getEventType() == event.CHARACTERS) {
                            writer.add(getNewCharactersEvent(m_eventFactory, event.asCharacters()));
                        } else {
                            writer.add(event);
                        }
                    }
                    writer.flush();
//                }
                   	
            	
//                XMLInputFactory factory = XMLInputFactory.newInstance();
//                XMLEventReader reader = null;
//                XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(out);
//				try {
//					//reader = factory.createXMLStreamReader(source);
//					reader = factory.createXMLEventReader(source);
//				} catch (XMLStreamException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
//                try {
//					while (reader.hasNext()) {
//					    int next = reader.next();
//					    switch (next) {
//					        case XMLStreamConstants.CHARACTERS: {
//					            String text = reader.getText().replaceAll("#\\{.+\\}", "TO_BE_DEFINED");
//
//					            out.write(text);
//					            break;
//					        }
//					        default:
//					        	out.write(reader.getText());
//					    }            	
//					}
//				} catch (XMLStreamException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//            	xsltFileStream = ProjectUtils.class.getResourceAsStream("/ReplaceTemplate.xsl");
//            	
//            	Source xsltSource = new StreamSource(xsltFileStream);            	
                // this allows us to use UTF-8 for storing data,
                // plus it checks any well-formedness issue in the submitted
                // data
//                Transformer t = TransformerFactory.newInstance().newTransformer();
//                t.transform(source, new StreamResult(out));
                
                
//            } catch (TransformerException e) {
//                throw new IOException2("Failed to persist configuration.xml", e);
            } catch (Exception e) {
            	throw new RuntimeException(e);  // FIXME Re-throw 
            } finally {
            	//xsltFileStream.close();
            	out.close();            	
            }

            // try to reflect the changes by reloading
            new XmlFile(Items.XSTREAM, out.getTemporaryFile()).unmarshal(project);
            project.onLoad(project.getParent(), project.getRootDir().getName());
            Jenkins.getInstance().rebuildDependencyGraph();

            // if everything went well, commit this new version
            out.commit();
            return ProjectUtils.findProject(project.getFullName());
        } finally {
            out.abort(); // don't leave anything behind
        }
    }

    public static List<Trigger<?>> getTriggers(AbstractProject implementationProject) {
        try {
            Field triggers = AbstractProject.class.getDeclaredField("triggers");
            triggers.setAccessible(true);
            Object result = triggers.get(implementationProject);
            //noinspection unchecked
            return (List<Trigger<?>>) result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
}
