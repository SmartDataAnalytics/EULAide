package uni.bonn.eula;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import uni.bonn.eula.controller.ClustersCreator;
import uni.bonn.eula.controller.OntologyBasedAnnotController;
import uni.bonn.eula.lib.GateResources;
import uni.bonn.eula.lib.StanfordResources;
import gate.*;
import gate.creole.ANNIEConstants;
import gate.creole.SerialAnalyserController;
import gate.creole.gazetteer.Gazetteer;
import gate.creole.ontology.*;
import gate.util.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Main class.
 *
 */
public class Main {
    // Base URI the Grizzly HTTP server will listen on
    public static final String BASE_URI = "http://0.0.0.0:8081/";
	


	
    /**
     * Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
     * @return Grizzly HTTP server.
     */
    public static HttpServer startServer() {
        // create a resource config that scans for JAX-RS resources and providers
        // in uni.bonn.eula package
        final ResourceConfig rc = new ResourceConfig().packages("uni.bonn.eula");
        rc.register(MultiPartFeature.class);


        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI),rc);
    }

	
    public Main(){
	      
		}
    /**
     * Main method.
     * @param args
     * @throws IOException
     * @throws GateException 
     */
   
    public static void main(String[] args) throws IOException, GateException , Exception{
        final HttpServer server = startServer();
        

		
        String discoDir = "";
        String gateHome = "";
        if(args.length > 0 ){
        	gateHome = args[0];
        	discoDir = args[1];

        }else{
        	gateHome = "gate";
        	discoDir = "EULA Word Space/DISCO-idx";

        }        
        GateResources gr = GateResources.getInstance(gateHome);

		gr.initializePipeLines( discoDir);
        StanfordResources sr = StanfordResources.getInstance();
        sr.initialize();
 
        System.out.println("Server started: http://0.0.0.0:8081/ ");
        
        System.out.println("Hit enter to stop it...");


        System.in.read();
        server.stop();

    }
}

