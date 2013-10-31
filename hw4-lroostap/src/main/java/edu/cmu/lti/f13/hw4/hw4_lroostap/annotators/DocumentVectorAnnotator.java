package edu.cmu.lti.f13.hw4.hw4_lroostap.annotators;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.IntegerArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.jcas.cas.FSList;
import org.uimafit.util.FSCollectionFactory;

import java.io.StringReader;
import java.util.*;

import edu.cmu.lti.f13.hw4.hw4_lroostap.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_lroostap.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_lroostap.utils.Utils;
import edu.cmu.lti.f13.hw4.hw4_lroostap.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_lroostap.typesystems.Token;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.CoreAnnotations.*;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.TokenizerFactory;

/**
 * This class creates term frequency vectors in three ways
 * FISRT one splits the text by space and calculates the frequency of each terms
 * SECOND one uses stanfordCoreNLP to generate terms as tokens which also splits 
 * and some other constraints like comma, etc. 
 * THIRD one uses stanfordCoreNLP tool to generate terms as lemmas and then removes
 * the stopwords from.
 * 
 * @author Laleh
 */

public class DocumentVectorAnnotator extends JCasAnnotator_ImplBase {
  
  @Override
  public void process(JCas jcas) throws AnalysisEngineProcessException {

    FSIterator<Annotation> iter = jcas.getAnnotationIndex().iterator();
    if (iter.isValid()) {
      iter.moveToNext();
      Document doc = (Document) iter.get();
      createTermFreqVector(jcas, doc);
    }

  }

  /**
   * This method creats the terms and their frequencies. It generates the lemmas and
   * removes the ones in the stopwords, then it calculates the frequency of each term
   * and updates the document tokenList, updates CAS and adds the tokens to it.  
   * @param jcas
   * @param doc
   */

  private void createTermFreqVector(JCas jcas, Document doc) {

    Map<String, Integer> tokenFrequency = new HashMap<String, Integer>();
    Map<String, Integer> stopwordsMap = new HashMap<String, Integer>();

    String stopwords = "# \\ $ a about above after again against all am an and any are aren't as at be because been before being below between both but by can't cannot could couldn't did didn't do does doesn't doing don't down during each few for from further had hadn't has hasn't have haven't having he he'd he'll he's her here here's hers herself him himself his how how's i i'd i'll i'm i've if in into is isn't it it's its itself let's me more most mustn't my myself no nor not of off on once only or other ought our ours ourselves out over own same shan't she she'd she'll she's should shouldn't so some such than that that's the their theirs them themselves then there there's these they they'd they'll they're they've this those through to too under until up very was wasn't we we'd we'll we're we've were weren't what what's when when's where where's which while who who's whom why why's with won't would wouldn't you you'd you'll you're you've your yours yourself yourselves";

    for (String stopword : stopwords.split(" ")) {
      stopwordsMap.put(stopword, 1);
    }

    
    // FIRST Way
    // Generating tokens based on splitting the text by space and counting the frequency
    
//    String[] tokList = doc.getText().toLowerCase().split(" ");
//    int c = 0;
//    for (String token : tokList) {
//      if (tokenFrequency.containsKey(token))
//        c = tokenFrequency.get(token) + 1;
//      else
//        c = 1;
//      tokenFrequency.put(token, c);
//    }
    // FIRST Way End

    
    
    // SECOND Way
    // Generating tokens via StanfordCoreNLP tokenizer and counting the frequency

//    TokenizerFactory<Word> factory = PTBTokenizerFactory.newTokenizerFactory();
//    Tokenizer<Word> tokenizer = factory.getTokenizer(new StringReader(doc.getText().toLowerCase()));
//    int c = 0;
//    for (Word token : tokenizer.tokenize()) {
//      if (tokenFrequency.containsKey(token.toString()))
//        c = tokenFrequency.get(token.toString()) + 1;
//      else
//        c = 1;
//      tokenFrequency.put(token.toString(), c);
//    }
    // SECOND Way End

    
    //THIRD way
    // Generating lemmas and their frequencies by calling StanfordCoreNLP tool
    // The code is based on the example in the StanfordCoreNLP site
    
    Properties props = new Properties();
    props.put("annotators", "tokenize, ssplit, pos, lemma");
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
    String text = doc.getText().toLowerCase();
    edu.stanford.nlp.pipeline.Annotation document = pipeline.process(text);
    List<edu.stanford.nlp.util.CoreMap> sentences = document.get(SentencesAnnotation.class);
    
    
    for (edu.stanford.nlp.util.CoreMap sentence : sentences) {
      for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
        String lemma = token.get(LemmaAnnotation.class);
        if (!stopwordsMap.containsKey(lemma)) {
          int c = 0;
          if (tokenFrequency.containsKey(lemma))
            c = tokenFrequency.get(lemma) + 1;
          else
            c = 1;
          tokenFrequency.put(lemma, c);
        }
      }
    }
    //THIRD way End

    
    // Generating Token annotations
    List<Token> tokenList = new ArrayList<Token>();
    for (Map.Entry<String, Integer> term : tokenFrequency.entrySet()) {
      Token newToken = new Token(jcas);
      newToken.setFrequency(term.getValue());
      newToken.setText(term.getKey());
      newToken.addToIndexes(jcas);
      tokenList.add(newToken);
    }

    // Converting the tokenList to FSList and setting the document tokenlist
    doc.setTokenList(Utils.fromCollectionToFSList(jcas, tokenList));
   
  }
}
