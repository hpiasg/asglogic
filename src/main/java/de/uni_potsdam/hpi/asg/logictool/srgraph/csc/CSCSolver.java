package de.uni_potsdam.hpi.asg.logictool.srgraph.csc;

import de.uni_potsdam.hpi.asg.common.stg.model.STG;

public interface CSCSolver {

    public boolean solveCSC(STG stgin, String stgoutfile);

}
