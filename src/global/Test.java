package global;

import methods.DataReader;
import models.MetaPath;
import utils.impl.QueryNodeExpandStrategyDivided;
import utils.impl.QueryNodeExpandStrategyOptimized;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * SHARED_TIMES: 1 -> 31s -> 18.7s
 * SHARED_TIMES: 2 -> 391s -> 104.3s
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


        // HomoBTruss homoBTruss = new HomoBTruss(graph, vertexType, edgeType);
        // Set<Integer> rsSet1 = homoBTruss.query(Config.queryNodeId, metaPath, Config.k);
        // System.out.println("finished");

    }
}
