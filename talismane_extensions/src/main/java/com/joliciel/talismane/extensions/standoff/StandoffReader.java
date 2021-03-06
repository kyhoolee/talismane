///////////////////////////////////////////////////////////////////////////////
//Copyright (C) 2014 Joliciel Informatique
//
//This file is part of Talismane.
//
//Talismane is free software: you can redistribute it and/or modify
//it under the terms of the GNU Affero General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//Talismane is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU Affero General Public License for more details.
//
//You should have received a copy of the GNU Affero General Public License
//along with Talismane.  If not, see <http://www.gnu.org/licenses/>.
//////////////////////////////////////////////////////////////////////////////
package com.joliciel.talismane.extensions.standoff;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.joliciel.talismane.LinguisticRules;
import com.joliciel.talismane.TalismaneException;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.corpus.AbstractAnnotatedCorpusReader;
import com.joliciel.talismane.machineLearning.Decision;
import com.joliciel.talismane.parser.DependencyArc;
import com.joliciel.talismane.parser.ParseConfiguration;
import com.joliciel.talismane.parser.ParserAnnotatedCorpusReader;
import com.joliciel.talismane.posTagger.PosTag;
import com.joliciel.talismane.posTagger.PosTagOpenClassIndicator;
import com.joliciel.talismane.posTagger.PosTagSequence;
import com.joliciel.talismane.posTagger.PosTagSet;
import com.joliciel.talismane.posTagger.PosTaggedToken;
import com.joliciel.talismane.posTagger.UnknownPosTagException;
import com.joliciel.talismane.rawText.Sentence;
import com.joliciel.talismane.sentenceAnnotators.SentenceAnnotator;
import com.joliciel.talismane.tokeniser.PretokenisedSequence;
import com.joliciel.talismane.tokeniser.Token;
import com.joliciel.talismane.tokeniser.TokenSequence;
import com.typesafe.config.Config;

public class StandoffReader extends AbstractAnnotatedCorpusReader implements ParserAnnotatedCorpusReader {
  private static final Logger LOG = LoggerFactory.getLogger(StandoffReader.class);
  private int sentenceCount = 0;
  private int lineNumber = 1;

  ParseConfiguration configuration = null;
  private int sentenceIndex = 0;

  private Map<String, StandoffToken> tokenMap = new HashMap<>();
  private Map<String, StandoffRelation> relationMap = new HashMap<>();
  private Map<String, StandoffRelation> idRelationMap = new HashMap<>();
  private Map<String, String> notes = new HashMap<String, String>();

  private List<List<StandoffToken>> sentences = new ArrayList<>();
  private final String punctuationDepLabel;

  public StandoffReader(Reader reader, Config config, TalismaneSession session) throws TalismaneException {
    super(config, session);
    PosTagSet posTagSet = session.getPosTagSet();
    punctuationDepLabel = session.getTransitionSystem().getDependencyLabelSet().getPunctuationLabel();

    Map<Integer, StandoffToken> sortedTokens = new TreeMap<>();
    try (Scanner scanner = new Scanner(reader)) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if (line.startsWith("T")) {

          String[] parts = line.split("[\\t]");
          String id = parts[0];
          String[] posTagParts = parts[1].split(" ");
          String posTagCode = posTagParts[0].replace('_', '+');
          int startPos = Integer.parseInt(posTagParts[1]);
          String text = parts[2];

          PosTag posTag = null;
          if (posTagCode.equalsIgnoreCase(PosTag.ROOT_POS_TAG_CODE)) {
            posTag = PosTag.ROOT_POS_TAG;
          } else {
            try {
              posTag = posTagSet.getPosTag(posTagCode);
            } catch (UnknownPosTagException upte) {
              throw new TalismaneException("Unknown posTag on line " + lineNumber + ": " + posTagCode);
            }
          }

          StandoffToken token = new StandoffToken();
          token.posTag = posTag;
          token.text = text;
          token.id = id;

          sortedTokens.put(startPos, token);
          tokenMap.put(id, token);

        } else if (line.startsWith("R")) {

          String[] parts = line.split("[\\t :]");
          String id = parts[0];
          String label = parts[1];
          String headId = parts[3];
          String dependentId = parts[5];
          StandoffRelation relation = new StandoffRelation();
          relation.fromToken = headId;
          relation.toToken = dependentId;
          relation.label = label;
          idRelationMap.put(id, relation);
          relationMap.put(dependentId, relation);
        } else if (line.startsWith("#")) {
          String[] parts = line.split("\t");
          String itemId = parts[1].substring("AnnotatorNotes ".length());
          String note = parts[2];
          notes.put(itemId, note);
        }
      }
    }

    for (String itemId : notes.keySet()) {
      String comment = notes.get(itemId);
      if (itemId.startsWith("R")) {
        StandoffRelation relation = idRelationMap.get(itemId);
        relation.comment = comment;
      } else {
        StandoffToken token = tokenMap.get(itemId);
        token.comment = comment;
      }
    }

    List<StandoffToken> currentSentence = null;
    for (StandoffToken token : sortedTokens.values()) {
      if (token.text.equals("ROOT")) {
        if (currentSentence != null)
          sentences.add(currentSentence);
        currentSentence = new ArrayList<StandoffReader.StandoffToken>();
      }
      currentSentence.add(token);
    }
    if (currentSentence != null)
      sentences.add(currentSentence);
  }

  @Override
  public boolean hasNextSentence() throws TalismaneException, IOException {
    if (this.getMaxSentenceCount() > 0 && sentenceCount >= this.getMaxSentenceCount()) {
      // we've reached the end, do nothing
    } else {
      if (configuration == null && sentenceIndex < sentences.size()) {

        List<StandoffToken> tokens = sentences.get(sentenceIndex++);

        LinguisticRules rules = session.getLinguisticRules();
        if (rules == null)
          throw new RuntimeException("Linguistic rules have not been set.");

        String text = "";
        for (StandoffToken standoffToken : tokens) {
          String word = standoffToken.text;
          // check if a space should be added before this
          // token

          if (rules.shouldAddSpace(text, word))
            text += " ";
          text += word;
        }
        Sentence sentence = new Sentence(text, session);

        for (SentenceAnnotator annotator : session.getSentenceAnnotators()) {
          annotator.annotate(sentence);
        }

        PretokenisedSequence tokenSequence = new PretokenisedSequence(sentence, session);
        PosTagSequence posTagSequence = new PosTagSequence(tokenSequence);
        Map<String, PosTaggedToken> idTokenMap = new HashMap<String, PosTaggedToken>();

        for (StandoffToken standoffToken : tokens) {
          Token token = tokenSequence.addToken(standoffToken.text);
          Decision posTagDecision = new Decision(standoffToken.posTag.getCode());
          PosTaggedToken posTaggedToken = new PosTaggedToken(token, posTagDecision, session);

          if (LOG.isTraceEnabled()) {
            LOG.trace(posTaggedToken.toString());
          }

          posTaggedToken.setComment(standoffToken.comment);

          posTagSequence.addPosTaggedToken(posTaggedToken);
          idTokenMap.put(standoffToken.id, posTaggedToken);
          LOG.debug("Found token " + standoffToken.id + ", " + posTaggedToken);
        }

        tokenSequence.setWithRoot(true);

        tokenSequence.finalise();

        configuration = new ParseConfiguration(posTagSequence);

        for (StandoffToken standoffToken : tokens) {
          StandoffRelation relation = relationMap.get(standoffToken.id);
          if (relation != null) {
            PosTaggedToken head = idTokenMap.get(relation.fromToken);
            PosTaggedToken dependent = idTokenMap.get(relation.toToken);
            if (head == null) {
              throw new TalismaneException("No token found for head id: " + relation.fromToken);
            }
            if (dependent == null) {
              throw new TalismaneException("No token found for dependent id: " + relation.toToken);
            }
            DependencyArc arc = configuration.addDependency(head, dependent, relation.label, null);
            arc.setComment(relation.comment);
          } else if (standoffToken.posTag.getOpenClassIndicator() == PosTagOpenClassIndicator.PUNCTUATION) {
            if (punctuationDepLabel != null) {
              PosTaggedToken dependent = idTokenMap.get(standoffToken.id);
              for (int i = dependent.getIndex() - 1; i >= 0; i--) {
                PosTaggedToken head = posTagSequence.get(i);
                if (head.getTag().getOpenClassIndicator() == PosTagOpenClassIndicator.PUNCTUATION)
                  continue;
                configuration.addDependency(head, dependent, punctuationDepLabel, null);
                break;
              }
            }
          }
        }

      }
    }
    return (configuration != null);
  }

  @Override
  public ParseConfiguration nextConfiguration() throws TalismaneException, IOException {
    ParseConfiguration nextConfiguration = null;
    if (this.hasNextSentence()) {
      nextConfiguration = configuration;
      configuration = null;
    }
    return nextConfiguration;
  }

  @Override
  public Map<String, String> getCharacteristics() {
    Map<String, String> attributes = new LinkedHashMap<String, String>();
    return attributes;
  }

  private static final class StandoffToken {
    public PosTag posTag;
    public String text;
    public String id;
    public String comment = "";
  }

  private static final class StandoffRelation {
    public String label;
    public String fromToken;
    public String toToken;
    public String comment = "";
  }

  @Override
  public PosTagSequence nextPosTagSequence() throws TalismaneException, IOException {
    return this.nextConfiguration().getPosTagSequence();
  }

  @Override
  public TokenSequence nextTokenSequence() throws TalismaneException, IOException {
    return this.nextConfiguration().getPosTagSequence().getTokenSequence();
  }

  @Override
  public Sentence nextSentence() throws TalismaneException, IOException {
    return this.nextConfiguration().getPosTagSequence().getTokenSequence().getSentence();
  }

  @Override
  public boolean isNewParagraph() {
    return false;
  }

}
