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
package com.joliciel.talismane;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import com.joliciel.talismane.languageDetector.LanguageDetectorProcessor;
import com.joliciel.talismane.parser.ParseConfigurationProcessor;
import com.joliciel.talismane.posTagger.PosTagSequenceProcessor;
import com.joliciel.talismane.sentenceDetector.SentenceProcessor;
import com.joliciel.talismane.tokeniser.TokenSequenceProcessor;

/**
 * An interface for processing a Reader from {@link TalismaneConfig#getReader()}
 * and writing the analysis result to a Writer from
 * {@link TalismaneConfig#getWriter()}.<br/>
 * Not thread-safe: a single Talismane cannot be used by multiple threads
 * simultaneously.<br/>
 * The output format is determined by the processor corresponding to
 * {@link TalismaneConfig#getEndModule()}.<br/>
 * This is accomplished by calling {@link #process()}.<br/>
 * 
 * @author Assaf Urieli
 *
 */
public interface Talismane {
	/**
	 * A module within the Talismane Suite.
	 * 
	 * @author Assaf Urieli
	 *
	 */
	public enum Module {
		LanguageDetector, SentenceDetector, Tokeniser, PosTagger, Parser
	}

	/**
	 * The command which Talismane is asked to perform.
	 * 
	 * @author Assaf Urieli
	 *
	 */
	public enum Command {
		/**
		 * Train a model using a corpus, a feature set, a classifier +
		 * parameters, etc.
		 */
		train,
		/**
		 * Analyse a corpus and add annotations.
		 */
		analyse,
		/**
		 * Evaluate an annotated corpus, by re-analysing the corpus and
		 * comparing the new annotations to the existing ones.
		 */
		evaluate,
		/**
		 * Process an annotated corpus - Talismane simply reads the corpus using
		 * the appropriate corpus reader and passes the results to the
		 * appropriate processors.
		 */
		process,
		/**
		 * Compare two annotated corpora.
		 */
		compare
	}

	public enum Option {
		/**
		 * Simply output what you read, usually changing the format.
		 */
		output,
		/**
		 * Test pos-tag features on a subset of words in the training set.
		 */
		posTagFeatureTester,
		/**
		 * Test parse features on the training set.
		 */
		parseFeatureTester
	}

	public enum Mode {
		/**
		 * Command line mode, reading from standard in or file, and writing to
		 * standard out or file.
		 */
		normal,
		/**
		 * Server listening on port, and processing input as it comes.
		 */
		server
	}

	/**
	 * Run the {@link Command} specified by
	 * {@link TalismaneConfigImpl#getCommand()}.
	 */
	public abstract void process();

	/**
	 * The language detector processor to be used if the end-module is the
	 * language detector.
	 */
	public LanguageDetectorProcessor getLanguageDetectorProcessor();

	public void setLanguageDetectorProcessor(LanguageDetectorProcessor languageDetectorProcessor);

	/**
	 * The sentence processor to be used if the end-module is the sentence
	 * detector.
	 */
	public SentenceProcessor getSentenceProcessor();

	public void setSentenceProcessor(SentenceProcessor sentenceProcessor);

	/**
	 * The token sequence processor to be used if the end-module is the
	 * tokeniser.
	 */
	public TokenSequenceProcessor getTokenSequenceProcessor();

	public void setTokenSequenceProcessor(TokenSequenceProcessor tokenSequenceProcessor);

	/**
	 * The pos-tag sequence processor to be used if the end-module is the
	 * pos-tagger.
	 */
	public PosTagSequenceProcessor getPosTagSequenceProcessor();

	public void setPosTagSequenceProcessor(PosTagSequenceProcessor posTagSequenceProcessor);

	/**
	 * The parse configuration processor to be used if the end-module is the
	 * parser.
	 */
	public ParseConfigurationProcessor getParseConfigurationProcessor();

	public void setParseConfigurationProcessor(ParseConfigurationProcessor parseConfigurationProcessor);

	/**
	 * If an error occurs during analysis, should Talismane stop immediately, or
	 * try to keep going with the next sentence? Default is true (stop
	 * immediately).
	 */
	public boolean isStopOnError();

	public void setStopOnError(boolean stopOnError);

	/**
	 * The reader to be used for input by this instance of Talismane.
	 */
	public Reader getReader();

	public void setReader(Reader reader);

	/**
	 * The writer to be used for output by this instance of Talismane.
	 * 
	 * @throws IOException
	 */
	public Writer getWriter() throws IOException;

	public void setWriter(Writer writer);
}