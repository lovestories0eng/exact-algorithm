package utils;

import models.MetaPath;

import java.util.Map;

public interface Decomposition {
    public Map<Integer, Integer> decompose(MetaPath queryMPath);//return the core number of each vertex
}
