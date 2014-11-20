package com.joelj.jenkins.eztemplates.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.logging.Logger;

public class TemplateVariablesUtils {
	
    private static final Logger LOG = Logger.getLogger("ez-templates");	
    

	public static Properties processProperties(String textarea) {
		Properties prop = new Properties();			

		StringReader reader = new StringReader(textarea);

		try {
			prop.load(reader);
		} catch (IOException e) {
			// FIXME Display error in validation form
		}
		
		return prop;
	}
	
	
	/* TODO 
	   + Optimize performance: traverse string just once, interpolating the value for each variable found
	   + Show interpolation feedback to the user (description below "Template Variables" text area):
	     - Variables found in template, but not in the implementation.
	     - Variables found in this implementation, but not defined in the template.
	*/
	public static String interpolateVariables(Properties prop, String input) {
		
		   for(String key : prop.stringPropertyNames()) {
			   
			  input = input.replaceAll("#\\{"+ key +"\\}", prop.getProperty(key));
			  
			  LOG.info(String.format("* Variable [%s] = [%s]", key, prop.getProperty(key)));
		   }		
		   
		   return input;
	}
	
}
