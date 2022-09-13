package utils;

import models.MetaPath;

import java.util.Map;

public interface Decomposition {
    public Map<Integer, Integer> decompose(MetaPath queryMPath);//return the core number of each vertex
    public int[] getReverseOrderArr();
    // return the vertex array, where vertices are sorted reversely
    // 返回顶点数组，其中顶点按相反顺序排序返回顶点数组，顶点按相反顺序排序
}
