package uni.bonn.eula.controller;
import gate.util.Out;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.stanford.nlp.coref.data.CorefChain;
import edu.stanford.nlp.coref.CorefCoreAnnotations;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.CharacterOffsetBeginAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import uni.bonn.eula.lib.AnnotEnum;
import uni.bonn.eula.lib.StanfordResources;
import uni.bonn.eula.model.*;
public class StanfordController {

	public Annotation getTokensLemma(String sentence){
		StanfordCoreNLP pipeline2 = StanfordResources.getPipeline2();
	    Annotation annotation = new Annotation(sentence);
	    pipeline2.annotate(annotation);
	    return annotation;
		
	}

	public Annotation getTokensInfo(String sentence){
		StanfordCoreNLP pipeline3 = StanfordResources.getPipeline3();
	    Annotation annotation = new Annotation(sentence);
	    pipeline3.annotate(annotation);
	    return annotation;
		
	}
	
	public String getAnnotations(String paragraph, long paragraphStartNodeOffset){
		String corefResolutionInfo = "";		
		StanfordCoreNLP pipeline = StanfordResources.getPipeline();

	    Annotation  annotation = new Annotation(paragraph);
	    
		
		
	    // run all the selected Annotators on this text
	    pipeline.annotate(annotation);
	    
	    
	    // display the new-style coreference graph
	    Map<Integer, CorefChain> corefChains = annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class);
	    if (corefChains != null ) {
	      List<CoreMap> sentences = annotation.get(CoreAnnotations.SentencesAnnotation.class);

	      for (Map.Entry<Integer,CorefChain> entry: corefChains.entrySet()) {	      	   
	        CorefChain.CorefMention representative = entry.getValue().getRepresentativeMention();
	        boolean outputHeading = false;
	        for (CorefChain.CorefMention mention : entry.getValue().getMentionsInTextualOrder())  {
	          if (mention == representative)
	            continue;
	          if (!outputHeading) {
	            outputHeading = true;
	          }
	          List<CoreLabel> tokens = sentences.get(mention.sentNum - 1).get(CoreAnnotations.TokensAnnotation.class);
	          	          
	          String originalText = mention.mentionSpan.toLowerCase();
	          if( !originalText.equals(representative.mentionSpan.toLowerCase()) && 
	        		  (originalText.contains("it") || originalText.contains("such") || originalText.contains("they") || originalText.contains("them") )){
		          long absoluteStartOffset = tokens.get(mention.startIndex - 1).beginPosition() + paragraphStartNodeOffset;
		          long absoluteEndOffset = tokens.get(mention.endIndex - 2).endPosition() + paragraphStartNodeOffset;	          
		          corefResolutionInfo +=  absoluteStartOffset + "," + absoluteEndOffset + "," + representative.mentionSpan;
		          corefResolutionInfo += ";";	          
	          }
	        }
	      }
	    }
	    if(corefResolutionInfo.length() > 0){
	    	corefResolutionInfo = corefResolutionInfo.substring(0, corefResolutionInfo.length()-1);
	    }
	    return corefResolutionInfo;
	}	


}
