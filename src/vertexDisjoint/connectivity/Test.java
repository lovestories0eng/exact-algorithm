package vertexDisjoint.connectivity;

import global.Config;
import methods.DataReader;
import models.MetaPath;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 优化方向：
 * 1：减少生成路径的数量
 * 2：减少生成的点对
 * **/
public class Test {
    public static void main(String[] args) {
        DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge, Config.dblpAttribute);
        int[][] graph = dataReader.readGraph();
        int[] vertexType = dataReader.readVertexType();
        int[] edgeType = dataReader.readEdgeType();
        double[][] attribute = dataReader.readVertexAttribute();
        HashMap<Map.Entry<Integer, Integer>, Integer> vertexPairMapEdge = dataReader.readVertexPairMapEdge();

        int[] vertex = {1, 0, 1}, edge = {3, 0};
        MetaPath metaPath = new MetaPath(vertex, edge);

        // int[] points = {101350};
        // int[] points = {0};
        int[] points = {2714};

        // int[] hops = {1, 2, 3, 4, 5, 6};
        int[] hops = {8};

        int[] sharedTimes = {2};

        // int[] sharedTimes = new int[103];
        // for (int i = 0; i <= 102; i++) {
        //     sharedTimes[i] = i + 1;
        // }


        for (int point : points) {
            System.out.println("点：" + point);
            Config.queryNodeId = point;
            for (int i : hops) {
                System.out.println("跳数：" + i);
                for (int sharedTime : sharedTimes) {
                    System.out.println("共享次数");
                    Config.SHARED_TIMES = sharedTime;

                    // 边不相交，存储边使用次数
                    int[] edgeUsedTimes = new int[edgeType.length];
                    int[] vertexUsedTime = new int[vertexType.length];
                    Arrays.fill(edgeUsedTimes, Config.SHARED_TIMES);
                    Arrays.fill(vertexUsedTime, Config.SHARED_TIMES);

                    // QueryNodeExpandStrategyOptimized queryNodeExpandStrategy = new QueryNodeExpandStrategyOptimized(graph, vertexType, edgeType, edgeUsedTimes, vertexUsedTime, vertexPairMapEdge, i);
                    QueryNodeExpandStrategyTest queryNodeExpandStrategy = new QueryNodeExpandStrategyTest(graph, vertexType, edgeType, edgeUsedTimes, vertexUsedTime, vertexPairMapEdge, i);
                    queryNodeExpandStrategy.query(Config.queryNodeId, metaPath, 1);
                }
            }
        }
    }
}
