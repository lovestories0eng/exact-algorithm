package global;

import methods.DataReader;
import models.MetaPath;
import utils.impl.QueryNodeExpandStrategyDivided;
import utils.impl.QueryNodeExpandStrategyOptimized;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * SHARED_TIMES: 1 -> 22.73s
 * SHARED_TIMES: 2 -> 159.63s
 * SHARED_TIMES: 3 -> 201.08s
 * SHARED_TIMES: 4 -> 545.12s
 * SHARED_TIMES: 5 -> 796.33s
 * SHARED_TIMES: 6 -> 800.55s
 * **/
public class Test {
    public static void main(String[] args) {
        DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
        int[][] graph = dataReader.readGraph();
        int[] vertexType = dataReader.readVertexType();
        int[] edgeType = dataReader.readEdgeType();
        HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge = dataReader.readVertexPairMapEdge();

        // 边不相交，存储边的使用次数
        int[] edgeUsedTimes = new int[edgeType.length];
        Arrays.fill(edgeUsedTimes, Config.SHARED_TIMES);

        int[] vertex = {1, 0, 1}, edge = {3, 0};
        MetaPath metaPath = new MetaPath(vertex, edge);

        // QueryNodeExpandStrategy queryNodeExpandStrategy = new QueryNodeExpandStrategy(graph, vertexType, edgeType, edgeUsedTimes, vertexPairMapEdge);
        QueryNodeExpandStrategyOptimized queryNodeExpandStrategy = new QueryNodeExpandStrategyOptimized(graph, vertexType, edgeType, edgeUsedTimes, vertexPairMapEdge);
        // QueryNodeExpandStrategyDivided queryNodeExpandStrategy = new QueryNodeExpandStrategyDivided(graph, vertexType, edgeType, edgeUsedTimes, vertexPairMapEdge);

        queryNodeExpandStrategy.query(Config.queryNodeId, metaPath);
    }
}
