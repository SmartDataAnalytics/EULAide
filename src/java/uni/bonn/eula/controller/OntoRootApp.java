package uni.bonn.eula.controller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import gate.CorpusController;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.ProcessingResource;
import gate.creole.ANNIEConstants;
import gate.creole.SerialAnalyserController;
import gate.util.GateException;

public class OntoRootApp {

	private CorpusController rootfinder = null;
	private static OntoRootApp instance = null;
	
		
	
    public  CorpusController createResources () throws GateException {
    	  

    	  try {
    	  
    	   //need Tools plugin for the Morphological Analyser 
    	   Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), "Tools").toURL());
    	  
    	   //Load ANNIE for the Tokeniser and POS Tagger
    	   Gate.getCreoleRegister().registerDirectories(new File(Gate.getPluginsHome(), ANNIEConstants.PLUGIN_DIR).toURL());
    	   
    	  
    	   
    	  } catch (GateException e) {
    	   // TODO Auto-generated catch block
    	   e.printStackTrace();
    	  } catch (MalformedURLException e) {
    	   // TODO Auto-generated catch block
    	   e.printStackTrace(); 
    	  } 
    	  
    	  String[] processingResources = {   
    	        "gate.creole.tokeniser.DefaultTokeniser",
    	        "gate.creole.POSTagger"
    	  };      
    	  
    	  rootfinder = (CorpusController)Factory.createResource("gate.creole.SerialAnalyserController");
    	   
    	  for(int pr = 0; pr < processingResources.length; pr++) {
    	         System.out.print("\t* Loading " + processingResources[pr] + " ... ");
    	         ((SerialAnalyserController)rootfinder).add((gate.LanguageAnalyser)Factory.createResource(processingResources[pr]));
    	         System.out.println("done");
    	       }
    	  
    	FeatureMap param = Factory.newFeatureMap();  
  		param = Factory.newFeatureMap();
/*  		
  		URL url = getClass().getResource("/improved_plugins/Tools/resources/morph/default.rul"); 
  		String filePath = url.getPath();	
  		param.put("rulesFile", filePath);  		
*/  		
  		param.put("rulesFile", getClass().getClassLoader().getResource("improved_plugins/Tools/resources/morph/default.rul").toExternalForm());
        System.out.print("\t* Loading Morphological Analyser ... ");
  		ProcessingResource morphAnalyzer =(ProcessingResource) Factory.createResource("gate.creole.morph.Morph", param);
  		((SerialAnalyserController)rootfinder).add(morphAnalyzer);	   
        System.out.println("done");
  
    	  
    	  rootfinder.setName("Root finder");
    	  return rootfinder;
    }
    
}
