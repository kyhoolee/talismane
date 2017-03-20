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
package com.joliciel.talismane.tokeniser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.joliciel.talismane.Talismane;
import com.joliciel.talismane.Talismane.BuiltInTemplate;
import com.joliciel.talismane.TalismaneSession;
import com.joliciel.talismane.output.FreemarkerTemplateWriter;
import com.joliciel.talismane.utils.ConfigUtils;
import com.typesafe.config.Config;

/**
 * Any class that can process token sequences generated by the tokeniser.
 * 
 * @author Assaf Urieli
 *
 */
public interface TokenSequenceProcessor extends Closeable {
	/**
	 * Process the next token sequence.
	 */
	public void onNextTokenSequence(TokenSequence tokenSequence);

	/**
	 * @param writer
	 *            if provided, the main processor will write to this writer, if
	 *            null, the outDir will be used instead
	 * @param outDir
	 * @param session
	 * @return
	 * @throws IOException
	 */
	public static List<TokenSequenceProcessor> getProcessors(Writer writer, File outDir, TalismaneSession session) throws IOException {
		List<TokenSequenceProcessor> processors = new ArrayList<>();

		Config config = session.getConfig();
		Config tokeniserConfig = config.getConfig("talismane.core.tokeniser");

		if (outDir != null)
			outDir.mkdirs();

		Reader templateReader = null;
		String configPath = "talismane.core.tokeniser.output.template";
		if (config.hasPath(configPath)) {
			templateReader = new BufferedReader(new InputStreamReader(ConfigUtils.getFileFromConfig(config, configPath)));
		} else {
			String tokeniserTemplateName = null;
			BuiltInTemplate builtInTemplate = BuiltInTemplate.valueOf(tokeniserConfig.getString("output.built-in-template"));
			switch (builtInTemplate) {
			case standard:
				tokeniserTemplateName = "tokeniser_template.ftl";
				break;
			case with_location:
				tokeniserTemplateName = "tokeniser_template_with_location.ftl";
				break;
			case with_prob:
				tokeniserTemplateName = "tokeniser_template_with_prob.ftl";
				break;
			default:
				throw new RuntimeException("Unknown builtInTemplate for tokeniser: " + builtInTemplate.name());
			}

			String path = "output/" + tokeniserTemplateName;
			InputStream inputStream = Talismane.class.getResourceAsStream(path);
			if (inputStream == null)
				throw new IOException("Resource not found in classpath: " + path);
			templateReader = new BufferedReader(new InputStreamReader(inputStream));
		}

		if (writer == null) {
			File file = new File(outDir, session.getBaseName() + "_tok.txt");
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), session.getOutputCharset()));
		}
		FreemarkerTemplateWriter templateWriter = new FreemarkerTemplateWriter(templateReader, writer);
		processors.add(templateWriter);
		return processors;
	}
}
