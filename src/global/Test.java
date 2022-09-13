package global;

import edgeDisjoint.HomoBTruss;
import methods.DataReader;
import models.MetaPath;

import java.util.Set;

public class Test {
    public static void main(String[] args) {
        DataReader dataReader = new DataReader(Config.dblpGraph, Config.dblpVertex, Config.dblpEdge);
        int[][] graph = dataReader.readGraph();
        int[] vertexType = dataReader.readVertexType();
        int[] edgeType = dataReader.readEdgeType();

        int[] vertex = {1, 0, 1}, edge = {3, 0};
        MetaPath metaPath = new MetaPath(vertex, edge);

        HomoBTruss homoBTruss = new HomoBTruss(graph, vertexType, edgeType);
        Set<Integer> rsSet1 = homoBTruss.query(Config.queryNodeId, metaPath, Config.k);
        System.out.println("finished");

    }
}
