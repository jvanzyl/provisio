tree grammar ProvisioModelGenerator;

options {
  tokenVocab = Provisio;
  output = AST;
  ASTLabelType = CommonTree;
}

@header {
package io.provis.parser.antlr;

import io.provis.Lookup;
import io.provis.model.*;
import io.provis.parser.*;

import java.io.File;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

}

@members {

  private Lookup locator;
  private File outputDirectory;
  private Map<String,String> versionMap; 

  public ProvisioModelGenerator(TreeNodeStream stream, Lookup locator, Map<String,String> versionMap, File outputDirectory) {
    super(stream); 
    this.locator = locator;
    this.versionMap = versionMap;
    this.outputDirectory = outputDirectory;
  }
}

runtime returns [ProvisioModel value]
  : ^(RUNTIME id=IDENTIFIER (^(VERSIONMAP versionMap=LITERAL))* artifactSets actions) {
      $value = new ProvisioModel($id.text, $versionMap.text, $artifactSets.value, $actions.value);     
    }
  ;

artifactSets returns [List<ArtifactSet> value = new ArrayList<ArtifactSet>()]
  : ( artifactSet { 
      $value.add($artifactSet.value); 
    } )*
  ;

artifactSet returns [ArtifactSet value]
  : ^(ARTIFACTSET directory=IDENTIFIER artifacts actions) { 
      $value = new ArtifactSet($directory.text, $artifacts.value, $actions.value, outputDirectory);
      
      for(ProvisioArtifact artifact : $artifacts.value) {
	      for(Action action : artifact.getActions()) {
          locator.setObjectProperty(action, "artifact", artifact);            	      
	        if($directory.text.equals("root")) {
	          locator.setObjectProperty(action, "fileSetDirectory", outputDirectory);            
           	  locator.setObjectProperty(action, "outputDirectory", outputDirectory);                             
           	  locator.setObjectProperty(action, "runtimeDirectory", outputDirectory);                             
	        } else {
	          locator.setObjectProperty(action, "fileSetDirectory", new File(outputDirectory, $directory.text));
              locator.setObjectProperty(action, "outputDirectory", new File(outputDirectory, $directory.text));
           	  locator.setObjectProperty(action, "runtimeDirectory", outputDirectory);                             
	        }
	      }            
      }
      
      for(Action action : $actions.value) {
	      if($directory.text.equals("root")) {
            locator.setObjectProperty(action, "fileSetDirectory", outputDirectory);     
            locator.setObjectProperty(action, "outputDirectory", outputDirectory);  
           	locator.setObjectProperty(action, "runtimeDirectory", outputDirectory);                                                                    
	      } else {
	        locator.setObjectProperty(action, "fileSetDirectory", new File(outputDirectory, $directory.text));
          	locator.setObjectProperty(action, "outputDirectory", new File(outputDirectory, $directory.text));
           	locator.setObjectProperty(action, "runtimeDirectory", outputDirectory);                                       	
	      }            
      }      
    }
  ;    
  
artifacts returns [List<ProvisioArtifact> value = new ArrayList<ProvisioArtifact>()]
  : ( artifact { 
      $value.add($artifact.value); 
    } )*
  ;  

artifact returns [ProvisioArtifact value]
  : ^(ARTIFACT c=COORDINATE actions) {
      String coordinate = $c.text;
      if(StringUtils.countMatches(coordinate, ":") == 1) {
      	coordinate = coordinate + ":" + versionMap.get(coordinate);
      }
      $value = new ProvisioArtifact(coordinate, $actions.value);
    }
  ;
  
actions returns [List<Action> value = new ArrayList<Action>()]
  : ( action { 
  	  //
  	  // If this is a runtime action try to set the runtimeDirectory property
  	  //
  	  locator.setObjectProperty($action.action, "runtimeDirectory", outputDirectory);                             	  	  	
      $value.add($action.action);
    } )*
  ;

action returns [Action action]
  : ^(ACTION k=IDENTIFIER v=assignments[locator.lookupAction($k.text)] ) {
      $action = $v.out;
    }
  ;  

assignments[Action action] returns [Action out]
  : ( assignment[$action] )+ {
      $out = $action;
    }
  ;
    
assignment[Action action]
  : ^(ASSIGNMENT property=IDENTIFIER value=expression) {  	   
	  //
	  // We know the name of the property for which we need to assign the value
	  // returned from the expression 
	  //    
	  locator.setObjectProperty(action, $property.text, $expression.value);
    }
  ;
    
expression returns [Object value]
  : BOOLEAN { $value = Boolean.valueOf($BOOLEAN.text);} 
  | LITERAL { $value = $LITERAL.text; }
  | DIGIT { $value = Integer.valueOf($DIGIT.text); }
  | {List elements = new ArrayList();}
    ^(LIST e=expression {elements.add($e.value);} (e=expression {elements.add($e.value);} )* )
    {$value = elements;}
  | { Map map = new HashMap();}
    ^(MAP k=LITERAL e=expression { map.put($k.text,$e.value); } (k=LITERAL e=expression { map.put($k.text,$e.value); } )* )
    {$value = map;}
  ;   
  
  