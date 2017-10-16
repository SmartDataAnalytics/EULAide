package uni.bonn.eula.lib;
import uni.bonn.eula.controller.OntoRootApp;
import uni.bonn.eula.controller.TermsConditionsApp;
import uni.bonn.eula.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.io.StringReader;

import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.models.lexparser.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import gate.util.Out;

/*
 * To use the Stanford Parser for every body that is received
 * @author : Kiran K.
 */
public class StanfordResources {


	private static StanfordResources instance = null;
	private static StanfordCoreNLP pipeline;
	private static StanfordCoreNLP pipeline2;
	private static StanfordCoreNLP pipeline3;	


	public void initialize()
	{
		try {
			
		    Properties props = new Properties();
		    props.setProperty("parse.maxlen", "55"); //previous = 55

		    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
		    pipeline = new StanfordCoreNLP(props);
		    props.setProperty("annotators", "tokenize, ssplit, pos , lemma");
		    pipeline2 = new StanfordCoreNLP(props);		    
		    props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse, natlog, openie");
		    pipeline3 = new StanfordCoreNLP(props);
		    
			Out.prln("Stanford Resources are loaded");
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}


	public static StanfordResources getInstance(){
		if(instance ==  null){
			instance = new StanfordResources();
		}
		return instance;
	}

	public static StanfordCoreNLP getPipeline(){
		return pipeline;
	}


	public static StanfordCoreNLP getPipeline3(){
		return pipeline3;
	}
	
	
	public static StanfordCoreNLP getPipeline2(){
		return pipeline2;
	}


}

