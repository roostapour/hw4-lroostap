package edu.cmu.lti.f13.hw4.hw4_lroostap.casconsumers;

import java.io.IOException;
import java.util.*;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;
import org.apache.uima.util.ProcessTrace;

import edu.cmu.lti.f13.hw4.hw4_lroostap.typesystems.Document;
import edu.cmu.lti.f13.hw4.hw4_lroostap.typesystems.Token;
import edu.cmu.lti.f13.hw4.hw4_lroostap.utils.Utils;

public class RetrievalEvaluator extends CasConsumer_ImplBase {

  /** query id number **/
  public ArrayList<Integer> qIdList;

  /** global vocabulary **/
  public HashMap<String, Integer> vocabMap;

  /** query and answer relevant values **/
  public ArrayList<Integer> relList;

  /** list of DocInf **/
  public ArrayList<DocInf> docInfArray;

  public void initialize() throws ResourceInitializationException {

    qIdList = new ArrayList<Integer>();
    relList = new ArrayList<Integer>();
    vocabMap = new HashMap<String, Integer>();
    docInfArray = new ArrayList<DocInf>();

  }

  /**
   * 1. construct the global word dictionary 2. keep the word frequency for each sentence
   */
  @Override
  public void processCas(CAS aCas) throws ResourceProcessException {

    JCas jcas;
    try {
      jcas = aCas.getJCas();
    } catch (CASException e) {
      throw new ResourceProcessException(e);
    }

    FSIterator it = jcas.getAnnotationIndex(Document.type).iterator();

    if (it.hasNext()) {
      Document newDocument = (Document) it.next();
      DocInf newDocInf = new DocInf();
      newDocInf.qId = newDocument.getQueryID();
      newDocInf.relValue = newDocument.getRelevanceValue();
      newDocInf.text = newDocument.getText();
      qIdList.add(newDocument.getQueryID());
      relList.add(newDocument.getRelevanceValue());

      FSList fsTokenList = newDocument.getTokenList();
      ArrayList<Token> tokenList = Utils.fromFSListToCollection(fsTokenList, Token.class);
      for (Token t : tokenList) {
        newDocInf.tokenVector.put(t.getText(), t.getFrequency());
        if (vocabMap.containsKey(t.getText()))
          vocabMap.put(t.getText(), t.getFrequency() + 1);
        else
          vocabMap.put(t.getText(), t.getFrequency());
      }

      docInfArray.add(newDocInf);

    }

  }

  /**
   * 1. Compute Cosine Similarity and rank the retrieved sentences 2. Compute the MRR metric
   */
  @Override
  public void collectionProcessComplete(ProcessTrace arg0) throws ResourceProcessException,
          IOException {

    super.collectionProcessComplete(arg0);

    // 1. Compute Cosine Similarity
    for (DocInf docFIt : docInfArray)
      if (docFIt.relValue == 99)
        for (DocInf docSIt : docInfArray)
          if (docSIt.relValue != 99 && docSIt.qId == docFIt.qId)
            docSIt.cosineSimilarity = computeCosineSimilarity(docFIt.tokenVector,
                    docSIt.tokenVector);

    // 1. Rank the retrieved sentences; Sorting the doc array based on the cosineSimilarity
    Collections.sort(docInfArray);
    int rank = 0;
    for (DocInf docFIt : docInfArray) {
      if (docFIt.relValue == 99) {
        System.out.println();
        rank = 1;
        for (DocInf docSIt : docInfArray)
          if (docSIt.relValue != 99)
            if (docSIt.qId == docFIt.qId) {
              docSIt.rank = rank;
              rank = rank + 1;
              if (docSIt.relValue == 1)
                System.out.format("Score: %.5f\t rank: %d   rel: %d   qid: %d\n",
                        docSIt.cosineSimilarity, docSIt.rank, docSIt.relValue, docSIt.qId);
            }
      }
    }

    // 2. Compute the MRR metric
    double metric_mrr = compute_mrr();
    System.out.format("\nMean Reciprocal Rank (MRR)::%.2f\n", metric_mrr);
  }

  /**
   * This method calculates the cosine similarity of two inputs attributes vectors
   * @return cosine_similarity
   */
  private double computeCosineSimilarity(Map<String, Integer> queryVector,
          Map<String, Integer> docVector) {

    double cosine_similarity = 0.0;
    double qdSigMultiply = 0.0;
    double querySigPart = 0.0;
    double docSigPart = 0.0;
    double multDen = 0.0;
    Map<String, Integer> docQVectorMap = new HashMap<String, Integer>();
    docQVectorMap.putAll(queryVector);
    docQVectorMap.putAll(docVector);

    for (String term : docQVectorMap.keySet()) {

      // Calculating the doc and query sigmaMultiply in numerator
      if (queryVector.containsKey(term) && docVector.containsKey(term))
        qdSigMultiply = qdSigMultiply + queryVector.get(term) * docVector.get(term);
      else
        qdSigMultiply = qdSigMultiply + 0;

      // Calculating the query sigma in denominator
      if (queryVector.containsKey(term))
        querySigPart = querySigPart + Math.pow(queryVector.get(term), 2);
      else
        querySigPart = querySigPart + 0;

      // Calculating the doc sigma in denominator
      if (docVector.containsKey(term))
        docSigPart = docSigPart + Math.pow(docVector.get(term), 2);
      else
        docSigPart = docSigPart + 0;

    }

    multDen = Math.sqrt(querySigPart) * Math.sqrt(docSigPart);
    cosine_similarity = qdSigMultiply / multDen;
    return cosine_similarity;
  }

  /**
   * This method calculates the Mean Reciprocal Rank (MRR) metric for tracking retrieval performance
   * 
   * @return mrr
   */

  private double compute_mrr() {
    double metric_mrr = 0.0;
    double sigRank = 0.0;
    int Q = 0;

    for (DocInf firstDoc : docInfArray)
      // Finding the query
      if (firstDoc.relValue == 99) {
        Q = Q + 1;
        for (DocInf secondDoc : docInfArray)
          if (secondDoc.relValue == 1)
            if (secondDoc.qId == firstDoc.qId)
              sigRank = sigRank + ((double) 1 / secondDoc.rank);
      }

    metric_mrr = sigRank / Q;
    return metric_mrr;
  }

}
