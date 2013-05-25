package io.provis.parser;

import io.provis.Lookup;
import io.provis.model.ProvisioModel;
import io.provis.parser.antlr.ProvisioLexer;
import io.provis.parser.antlr.ProvisioModelGenerator;
import io.provis.parser.antlr.ProvisioParser;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.antlr.runtime.ANTLRReaderStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.BufferedTreeNodeStream;
import org.antlr.runtime.tree.Tree;
import org.antlr.runtime.tree.TreeNodeStream;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;

@Named
@Singleton
public class ProvisioModelParser {

  private Lookup lookup;

  @Inject
  public ProvisioModelParser(Lookup lookup) {
    this.lookup = lookup;
  }

  public Lookup getLookup() {
    return lookup;
  }
  
  public ProvisioModel read(File modelFile, File outputDirectory, Map<String, String> versionMap) throws IOException {
    return read(Files.newReaderSupplier(modelFile, Charsets.UTF_8), outputDirectory, versionMap);    
  }

  public ProvisioModel read(InputSupplier<? extends Reader> input, File outputDirectory, Map<String, String> versionMap) throws IOException {
    Tree tree = parseTree(input);
    TreeNodeStream stream = new BufferedTreeNodeStream(tree);
    ProvisioModelGenerator generator = new ProvisioModelGenerator(stream, lookup, versionMap, outputDirectory);
    try {
      return generator.runtime().value;
    } catch (RecognitionException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private Tree parseTree(InputSupplier<? extends Reader> input) throws IOException {
    ProvisioLexer lexer = new ProvisioLexer(new ANTLRReaderStream(input.getInput()));
    ProvisioParser parser = new ProvisioParser(new CommonTokenStream(lexer));
    try {
      Tree tree = (Tree) parser.runtime().getTree();
      if (parser.getNumberOfSyntaxErrors() > 0) {
        throw new IllegalArgumentException("syntax error");
      }
      return tree;
    } catch (RecognitionException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
