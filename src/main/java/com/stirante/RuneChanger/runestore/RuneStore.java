package com.stirante.RuneChanger.runestore;

import com.stirante.RuneChanger.model.Champion;
import com.stirante.RuneChanger.model.RunePage;

import java.util.ArrayList;
import java.util.List;

public class RuneStore {

    private static final List<RuneSource> sources = new ArrayList<>();

    static {
        sources.add(new RuneforgeSource());
        sources.add(new LocalSource());
    }

    /**
     * Get list of rune pages for champion
     *
     * @param champion champion
     * @return list of rune pages
     */
    public static List<RunePage> getRunes(Champion champion) {
        ArrayList<RunePage> result = new ArrayList<>();
        for (RuneSource source : sources) {
            result.addAll(source.getForChampion(champion));
        }
        return result;
    }

}
