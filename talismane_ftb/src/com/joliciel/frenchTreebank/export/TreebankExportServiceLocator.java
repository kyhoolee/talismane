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
package com.joliciel.frenchTreebank.export;

import com.joliciel.frenchTreebank.TreebankServiceLocator;
import com.joliciel.talismane.machineLearning.MachineLearningServiceLocator;

public class TreebankExportServiceLocator {
	private TreebankExportServiceImpl treebankUploadService;
	private TreebankServiceLocator treebankServiceLocator;
	private MachineLearningServiceLocator machineLearningServiceLocator;

    
	public TreebankExportServiceLocator(TreebankServiceLocator treebankServiceLocator) {
		this.treebankServiceLocator = treebankServiceLocator;
		this.machineLearningServiceLocator = MachineLearningServiceLocator.getInstance();
	}


	public TreebankExportService getTreebankExportService() {
		if (treebankUploadService==null) {
			treebankUploadService = new TreebankExportServiceImpl();
			treebankUploadService.setTreebankService(treebankServiceLocator.getTreebankService());
			treebankUploadService.setTokeniserService(treebankServiceLocator.getTokeniserService());
			treebankUploadService.setPosTaggerService(treebankServiceLocator.getPosTaggerService());
			treebankUploadService.setFilterService(treebankServiceLocator.getFilterService());
			treebankUploadService.setTokenFilterService(treebankServiceLocator.getTokenFilterService());
			treebankUploadService.setTalismaneService(treebankServiceLocator.getTalismaneService());
			treebankUploadService.setMachineLearningService(machineLearningServiceLocator.getMachineLearningService());
		}
		return treebankUploadService;
	}  
    
}
