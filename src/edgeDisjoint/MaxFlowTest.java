package edgeDisjoint;

import common.BatchLinker;
import global.Config;
import methods.DataReader;
import models.MetaPath;
import utils.impl.MaxFlowStrategy;

import java.util.*;

public class MaxFlowTest {

    public static void main(String[] args) {
        DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge, Config.dblpAttribute);
        int[][] graph = dataReader.readGraph();
        int[] vertexType = dataReader.readVertexType();
        int[] edgeType = dataReader.readEdgeType();
        HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge = dataReader.readVertexPairMapEdge();

        // 边不相交，存储边的使用次数
        int[] edgeUsedTimes = new int[edgeType.length];
        Arrays.fill(edgeUsedTimes, Config.SHARED_TIMES);

        int[] vertex = {1, 0, 1}, edge = {3, 0};
        MetaPath metaPath = new MetaPath(vertex, edge);

        MaxFlowStrategy maxFlowStrategy = new MaxFlowStrategy(graph, vertexType, edgeType, edgeUsedTimes, vertexPairMapEdge);
        maxFlowStrategy.query(Config.queryNodeId, metaPath);
    }
}
