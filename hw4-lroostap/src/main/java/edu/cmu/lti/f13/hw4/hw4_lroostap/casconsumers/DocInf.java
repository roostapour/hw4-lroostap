package edu.cmu.lti.f13.hw4.hw4_lroostap.casconsumers;

import java.util.HashMap;

public class DocInf implements Comparable<DocInf> {


  /** document cosine similarity **/
  public double cosineSimilarity = 0.0;

  /** document cosine rank **/
  public Integer rank = 0;

  /** document relevance number **/
  public Integer relValue = 0;
  
  /** query id **/
  public Integer qId = 0;
  
  /** document text **/
  public String text;

  /** document token frequency vector **/
  public HashMap<String, Integer> tokenVector = new HashMap<String, Integer>();
  
  
  // Implementing a comparator to be able to sort DocInf objects
  public int compareTo(DocInf B) {
    if (this.cosineSimilarity > B.cosineSimilarity)
      return -1;
    if (this.cosineSimilarity < B.cosineSimilarity)
      return 1;
    return 0;
  }

}