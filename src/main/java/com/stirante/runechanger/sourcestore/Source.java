package com.stirante.runechanger.sourcestore;

public interface Source {

    /**
     * Returns friendly name of the source
     */
    String getSourceName();

    /**
     * Returns settings key of the source
     */
    String getSourceKey();
}
